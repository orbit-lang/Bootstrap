package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

abstract class DeclNode<D: Decl> : Node() {
    abstract fun getDecl(env: Env) : D
}

data class TypeLiteralNode(override val firstToken: Token, override val lastToken: Token, val typeId: String) : DeclNode<Decl.Type>() {
    override fun getChildren(): List<Node> = emptyList()
    override fun toString(): String = typeId
    override fun getDecl(env: Env): Decl.Type = Decl.Type(IType.Type(typeId), emptyMap())
}
