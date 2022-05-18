package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.BlockNode
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
        val params = input.node.signature.parameterNodes.zip(signature.parameters)

        params.forEach {
            nInferenceUtil.bind(it.first.identifierNode.identifier, it.second)
        }

        return when (val result = BlockInference.infer(nInferenceUtil, TypeAnnotatedInferenceContext(signature.returns, BlockNode::class.java), input.node.body)) {
            is InferenceResult.Success<*> -> {
                if (signature.returns is Infer) {
                    // The signature is telling us to update its return type as soon as we can infer it from the body
                    // e.g. For `(Foo) foo _ _ = Foo()`, the return Type can be inferred as `Foo`
                    val nSignature = signature.withInferredReturnType(result.type)

                    input.inferenceUtil.replace(signature, nSignature)
                }

                result.type
            }
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