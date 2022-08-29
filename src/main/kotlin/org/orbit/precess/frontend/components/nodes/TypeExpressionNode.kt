package org.orbit.precess.frontend.components.nodes

import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.utils.AnyType

abstract class TypeExpressionNode : Node() {
    abstract fun infer(env: Env) : AnyType
}