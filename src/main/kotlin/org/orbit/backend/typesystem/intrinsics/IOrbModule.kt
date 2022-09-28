package org.orbit.backend.typesystem.intrinsics

import org.orbit.backend.typesystem.components.Decl
import org.orbit.backend.typesystem.components.Env
import org.orbit.precess.backend.components.Expr
import org.orbit.backend.typesystem.components.IType

interface IOrbModule {
    fun getPublicTypes() : List<IType.Type>
    fun getPublicTypeAliases() : List<IType.Alias>
    fun getPublicOperators() : List<IType.IOperatorArrow<*, *>>
}

private fun IOrbModule.getTypeDecls() : List<Decl.Type>
    = getPublicTypes().map { Decl.Type(it, emptyMap()) }

private fun IOrbModule.getTypeAliasDecls() : List<Decl.TypeAlias>
    = getPublicTypeAliases().map { Decl.TypeAlias(it.name, Expr.AnyTypeLiteral(it.type)) }

private fun IOrbModule.getOperatorDecls() : List<Decl.Operator>
    = getPublicOperators().map { Decl.Operator(it) }

fun IOrbModule.getPublicAPI() : Env = Env()
    .extendAll(getTypeDecls())
    .extendAll(getTypeAliasDecls())
    .extendAll(getOperatorDecls())