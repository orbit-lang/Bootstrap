package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.AnySerializable
import org.orbit.core.OrbitMangler
import org.orbit.core.SerialSignature
import org.orbit.core.nodes.Annotations
import org.orbit.core.nodes.MethodCallNode
import org.orbit.core.phase.flatMapNotNull
import org.orbit.graph.extensions.annotate
import org.orbit.types.next.components.*
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer

object MethodCallInference : Inference<MethodCallNode, TypeComponent>, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: MethodCallNode): InferenceResult {
        val receiver = inferenceUtil.infer(node.receiverExpression)

        if (node.isPropertyAccess) {
            if (receiver is FieldAwareType) {
                val matches = receiver.getFields()
                    .filter { it.name == node.messageIdentifier.identifier }

                if (matches.isEmpty())
                    throw invocation.make<TypeSystem>("Receiver ${receiver.toString(printer)} does not expose a Field named ${printer.apply(node.messageIdentifier.identifier, PrintableKey.Bold, PrintableKey.Italics)}", node.messageIdentifier.firstToken)

                val t = matches.first().type

                val fType = when (t) {
                    is IConstantValue<*> -> t
                    else -> inferenceUtil.find(matches.first().type.fullyQualifiedName)
                } ?: TODO("HERE?!?!?!")

                return fType.inferenceResult()
            } else {
                throw invocation.make<TypeSystem>("Cannot invoke Field `${printer.apply(node.messageIdentifier.identifier, PrintableKey.Italics, PrintableKey.Bold)}` on Type ${receiver.toString(printer)} (${receiver.kind.toString(printer)})", node.receiverExpression)
            }
        }

        var candidateSignatures = inferenceUtil.getTypeMap().filter {
            it is Signature
                && it.relativeName == node.messageIdentifier.identifier
                && AnyEq.eq(inferenceUtil.toCtx(), receiver, it.receiver)
        }

        if (candidateSignatures.isEmpty()) {
            // TODO - Extensions on anything
            val mono = receiver as? MonomorphicType<TypeComponent>
                ?: throw invocation.make<TypeSystem>("Receiver ${receiver.toString(printer)} does not expose a method named ${printer.apply(node.messageIdentifier.identifier, PrintableKey.Italics, PrintableKey.Bold)}", node.messageIdentifier.firstToken)

            val extensions = inferenceUtil.getExtensions(mono.polymorphicType)

            val candidateExtensions = extensions.mapNotNull {
                when (it.extend(inferenceUtil, mono)) {
                    is Never -> null
                    else -> it
                }
            }

            candidateSignatures = candidateExtensions.flatMap {
                it.signatures.mapNotNull { s -> when (s.relativeName) {
                    node.messageIdentifier.identifier -> s.sub(mono.polymorphicType, mono)
                    else -> null
                }}
            }

            if (candidateSignatures.isEmpty()) {
                throw invocation.make<TypeSystem>("Receiver ${receiver.toString(printer)} does not expose a method named ${printer.apply(node.messageIdentifier.identifier, PrintableKey.Italics, PrintableKey.Bold)}", node.messageIdentifier.firstToken)
            }
        }

        if (candidateSignatures.count() > 1) {
            val pretty = candidateSignatures.joinToString("\n\t") { it.toString(printer) }

            throw invocation.make<TypeSystem>("Multiple candidates found for method call:\n\t$pretty", node.messageIdentifier.firstToken)
        }

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
         * If the virtual Type is found to implement the virtual Trait, then this is a valid call to this method.
         */
        val callableInterface = signature.derive()
        val calleeContracts = arguments.mapIndexed { idx, type -> Field("$idx", type) }
        val receiverContract = Field("__receiver", receiver)
        val returnsContract = Field("__returns", signature.returns)
        val calleeType = Type(signature.getPath(OrbitMangler) + "SyntheticCallee", calleeContracts + receiverContract + returnsContract)

        val onFailure = {
            val signaturePretty = signature.parameters.joinToString(", ") { it.toString(printer) }
            val calleePretty = arguments.joinToString(", ") { it.toString(printer) }

            Never("Cannot call method ${signature.toString(printer)} with arguments ($calleePretty), expected ($signaturePretty)")
        }

        if (callableInterface.contracts.count() != calleeType.getFields().count())
            return onFailure().inferenceResult()

        node.annotate(SerialSignature(signature), Annotations.Signature)

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