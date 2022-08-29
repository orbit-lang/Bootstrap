package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.utils.AnyType

data class UnboxNode(override val firstToken: Token, override val lastToken: Token, val box: TermExpressionNode<*>) : TypeExpressionNode() {
    override fun getChildren(): List<Node> = listOf(box)
    override fun toString(): String = "$box"

    override fun infer(env: Env): AnyType {
        val boxedType = box.getExpression(env).infer(env)

        if (boxedType !is IType.Box) return boxedType

        return boxedType.generator.infer(env)
    }
}
