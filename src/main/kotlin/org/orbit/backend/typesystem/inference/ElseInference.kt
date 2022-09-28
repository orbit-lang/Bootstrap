package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.core.nodes.ElseNode

object ElseInference : ITypeInference<ElseNode>, KoinComponent {
    override fun infer(node: ElseNode, env: Env): AnyType
        = env.getMatchType()
}