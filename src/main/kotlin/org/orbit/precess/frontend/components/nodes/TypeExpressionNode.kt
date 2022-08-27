package org.orbit.precess.frontend.components.nodes

import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

abstract class TypeExpressionNode<T: IType<T>> : Node() {
    abstract fun infer(env: Env) : T
}