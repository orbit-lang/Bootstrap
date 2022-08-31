package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.INode
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.components.TypeAttribute

sealed interface DeclResult<D: Decl> {
    data class Success<D: Decl>(val decl: D) : DeclResult<D>
    data class Failure<D: Decl>(val reason: IType.Never) : DeclResult<D>
}

fun <D: Decl> D.toSuccess() : DeclResult.Success<D> = DeclResult.Success(this)
operator fun DeclResult<*>.plus(other: DeclResult<*>) : DeclResult<Decl.Compound<*, *>> = when (this) {
    is DeclResult.Success -> when (other) {
        is DeclResult.Success -> DeclResult.Success(Decl.Compound(decl, other.decl))
        is DeclResult.Failure -> DeclResult.Failure(other.reason)
    }

    is DeclResult.Failure -> when (other) {
        is DeclResult.Success -> DeclResult.Failure(this.reason)
        is DeclResult.Failure -> DeclResult.Failure(reason + other.reason)
    }
}

operator fun List<DeclResult<*>>.unaryPlus() : DeclResult<*>
    = reduce { acc, next -> acc + next }

interface DeclNode<D: Decl> : INode {
    fun getDecl(env: Env) : DeclResult<D>
}

data class TypeLiteralNode(override val firstToken: Token, override val lastToken: Token, val typeId: String, val attributes: List<TypeAttribute> = emptyList()) : DeclNode<Decl.Type> {
    override fun getChildren(): List<INode> = emptyList()
    override fun toString(): String = typeId
    override fun getDecl(env: Env): DeclResult<Decl.Type>
        = Decl.Type(IType.Type(typeId, attributes), emptyMap()).toSuccess()
}
