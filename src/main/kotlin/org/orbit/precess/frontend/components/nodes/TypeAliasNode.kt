package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.INode
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Env

data class TypeAliasNode(override val firstToken: Token, override val lastToken: Token, val ref: String, val term: TermExpressionNode<*>) : DeclNode<Decl.TypeAlias> {
    override fun getChildren(): List<INode> = listOf(term)
    override fun toString(): String = "$ref:$term"

    override fun getDecl(env: Env): DeclResult<Decl.TypeAlias>
        = DeclResult.Success(Decl.TypeAlias(ref, term.getExpression(env)))
}

