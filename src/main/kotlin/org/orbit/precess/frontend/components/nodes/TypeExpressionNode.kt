package org.orbit.precess.frontend.components.nodes

import org.orbit.core.nodes.INode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.utils.AnyType

interface TypeExpressionNode : IPrecessNode {
    fun infer(env: Env) : AnyType
}