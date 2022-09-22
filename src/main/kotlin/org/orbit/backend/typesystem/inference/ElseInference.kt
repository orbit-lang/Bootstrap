package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.orbit.core.nodes.ElseNode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

object ElseInference : ITypeInference<ElseNode>, KoinComponent {
    override fun infer(node: ElseNode, env: Env): IType<*>
        = env.getMatchType()
}