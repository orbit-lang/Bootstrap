package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.MethodDefNode
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.types.next.components.*
import org.orbit.types.next.inference.TypeAnnotatedInferenceContext
import org.orbit.types.next.inference.BlockInference
import org.orbit.types.next.inference.InferenceResult
import org.orbit.util.Invocation
import org.orbit.util.Printer

object MethodBodyPhase : TypePhase<MethodDefNode, TypeComponent>, KoinComponent {
    override val invocation: Invocation by inject()
    private val printer: Printer by inject()

    private fun typeCheckGenericMethodBody(input: TypePhaseData<MethodDefNode>, signature: PolymorphicType<ISignature>) : TypeComponent {
        TODO("Generic Methods")
    }

    private fun typeCheckMethodBody(input: TypePhaseData<MethodDefNode>, signature: Signature) : TypeComponent {
        val nInferenceUtil = input.inferenceUtil.derive(self = signature.receiver)

        return when (val result =
            BlockInference.infer(nInferenceUtil, TypeAnnotatedInferenceContext(signature.returns), input.node.body)) {
            is InferenceResult.Success<*> -> result.type
            is InferenceResult.Failure -> result.never
        }
    }

    override fun run(input: TypePhaseData<MethodDefNode>): TypeComponent {
        val signature = input.inferenceUtil.inferAs<MethodSignatureNode, ISignature>(input.node.signature)

        return when (signature) {
            is PolymorphicType<*> -> typeCheckGenericMethodBody(input, signature as PolymorphicType<ISignature>)
            is Signature -> typeCheckMethodBody(input, signature)
            else -> Never("${signature.toString(printer)} is not a Signature")
        }
    }
}