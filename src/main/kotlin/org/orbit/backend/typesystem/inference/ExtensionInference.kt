package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.ExtensionNode
import org.orbit.precess.backend.components.Decl.Signature
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.util.Invocation

object ExtensionInference : ITypeInference<ExtensionNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: ExtensionNode, env: Env): IType<*> {
        val targetType = TypeSystemUtils.infer(node.targetTypeNode, env)
        val body = TypeSystemUtils.inferAll(node.bodyNodes, env)
        val xSignatureDecls = body.filterIsInstance<IType.Signature>()
            .map(::Signature)

        env.extendAllInPlace(xSignatureDecls)

        return targetType
    }
}