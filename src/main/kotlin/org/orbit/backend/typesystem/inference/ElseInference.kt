package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.CaseTypeEnvironment
import org.orbit.core.nodes.ElseNode

object ElseInference : ITypeInference<ElseNode, CaseTypeEnvironment> {
    override fun infer(node: ElseNode, env: CaseTypeEnvironment): AnyType
        = env.match
}