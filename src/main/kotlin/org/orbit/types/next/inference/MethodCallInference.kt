package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.nodes.MethodCallNode
import org.orbit.types.next.components.*
import org.orbit.util.Printer

object MethodCallInference : Inference<MethodCallNode, TypeComponent>, KoinComponent {
    private val printer: Printer by inject()

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: MethodCallNode): InferenceResult {
        val receiver = inferenceUtil.infer(node.receiverExpression)
        val leadingPath = receiver.getPath(OrbitMangler) + node.messageIdentifier.identifier
        val candidateSignatures = inferenceUtil.getTypeMap().filter {
            it is Signature && it.getPath(OrbitMangler).containsSubPath(leadingPath)
        }

        if (candidateSignatures.count() != 1) TODO("Method Call Conflict")

        val signature = candidateSignatures[0] as Signature
        val arguments = inferenceUtil.inferAll(node.parameterNodes, AnyExpressionContext)

        /**
         * NOTE - This idea is kind of nuts, but it seems to work pretty well.
         *
         * We start by synthesising a new "virtual" Trait with a FieldContract for each expected parameter,
         * plus one each for the receiver & return types.
         *
         * We then synthesise a new, corresponding "virtual" Type consisting of the known type info from the CallSite.
         *
         * If the virtual Type is found to implement the virtual Trait, then this is valid call to this method.
         */

        val callableInterface = signature.derive()
        val calleeContracts = arguments.mapIndexed { idx, type -> Field("$idx", type) }
        val receiverContract = Field("__receiver", receiver)
        val returnsContract = Field("__returns", Anything)
        val calleeType = Type(signature.getPath(OrbitMangler) + "SyntheticCallee", calleeContracts + receiverContract + returnsContract)

        val onFailure = {
            val signaturePretty = signature.parameters.map { it.toString(printer) }.joinToString(", ")
            val calleePretty = arguments.joinToString(", ") { it.toString(printer) }

            Never("Cannot call method ${signature.toString(printer)} with arguments ($calleePretty), expected ($signaturePretty)")
        }

        if (callableInterface.contracts.count() != calleeType.getFields().count())
            return onFailure().inferenceResult()

        return when (val r = callableInterface.isImplemented(inferenceUtil.toCtx(), calleeType)) {
            is ContractResult.Success -> signature.returns
            is ContractResult.Group -> when (r.isSuccessGroup) {
                true -> signature.returns
                else -> onFailure()
            }
            else -> onFailure()
        }.inferenceResult()
    }
}