package org.orbit.precess.frontend.components.nodes

import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.utils.AnyType

interface TypeExpressionNode : IPrecessNode {
    fun infer(env: Env) : AnyType
}