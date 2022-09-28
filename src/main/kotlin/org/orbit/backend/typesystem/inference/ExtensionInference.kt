package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Decl.Signature
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.ContextExpressionNode
import org.orbit.core.nodes.ExtensionNode
import org.orbit.util.Invocation

object ExtensionInference : ITypeInference<ExtensionNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: ExtensionNode, env: Env): AnyType {
        val explicitContext = when (val ctx = node.context) {
            null -> null
            else -> TypeSystemUtils.inferAs<ContextExpressionNode, Env>(node.context, env)
        }

//        val targetEvidence = TypeSystemUtils.gatherEvidence(node.targetTypeNode)
//        val evidence = TypeSystemUtils.gatherAllEvidence(node.bodyNodes)
//        val targetContext = targetEvidence.evidence.firstOrNull()?.first as? Env
//
//        val nEnv = when (targetContext) {
//            null -> evidence.context
//            else -> targetContext
//        }
//
//        println("Context inference: $nEnv")

        val nEnv = when (explicitContext) {
            null -> env
            else -> explicitContext
        }

        val targetType = TypeSystemUtils.infer(node.targetTypeNode, nEnv)
        val body = TypeSystemUtils.inferAll(node.bodyNodes, nEnv)
        val xSignatureDecls = body.filterIsInstance<IType.Signature>()
            .map(::Signature)

        env.extendAllInPlace(xSignatureDecls)

        return targetType
    }
}