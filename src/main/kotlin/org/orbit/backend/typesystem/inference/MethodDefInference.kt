package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.MethodDefNode
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.util.Invocation

object MethodDefInference : ITypeInference<MethodDefNode, IMutableTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: MethodDefNode, env: IMutableTypeEnvironment): AnyType {
        val nEnv = LocalEnvironment(env)
        val mEnv = when (val n = node.context) {
            null -> nEnv
            else -> ContextualTypeEnvironment(env, TypeInferenceUtils.inferAs(n, nEnv))
        }

        val signature = TypeInferenceUtils.inferAs<MethodSignatureNode, Signature>(node.signature, mEnv, parametersOf(false))
        val oEnv = AnnotatedSelfTypeEnvironment(mEnv, signature, signature.returns)

        if (node.signature.isInstanceMethod) {
            oEnv.bind(node.signature.receiverIdentifier!!.identifier, signature.receiver, node.signature.receiverIdentifier!!.index)
        }

        node.signature.getAllParameterPairs().forEach {
            val type = TypeInferenceUtils.infer(it.typeExpressionNode, oEnv)

            oEnv.bind(it.identifierNode.identifier, type, it.identifierNode.index)
        }

        val returns = TypeInferenceUtils.infer(node.body, oEnv)

        return when (TypeUtils.checkEq(oEnv, returns, signature.returns)) {
            true -> signature
            else -> throw invocation.make<TypeSystem>("Method `${node.signature.identifierNode.identifier}` declares return Type of `${signature.returns}`, found `$returns`", node)
        }
    }
}