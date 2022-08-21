package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.utils.AnyType

data class TypeLookupNode(override val firstToken: Token, override val lastToken: Token, val context: ContextLiteralNode, val type: TypeLiteralNode) : TypeExprNode() {
    override fun getChildren(): List<Node> = listOf(context, type)
    override fun toString(): String = "$context.$type"

    override fun infer(interpreter: Interpreter, env: Env): AnyType {
        val nEnv = context.infer(interpreter, env) as Env

        return nEnv.getElement(type.typeId) ?: IType.Never("Unknown Type $type in Context $context")
    }
}
