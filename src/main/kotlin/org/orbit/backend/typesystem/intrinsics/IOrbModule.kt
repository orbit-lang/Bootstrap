package org.orbit.backend.typesystem.intrinsics

import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

interface IOrbModule {
    fun getPublicTypes() : List<IType.Type>
    fun getPublicOperators() : List<IType.IOperatorArrow<*, *>>
}

private fun IOrbModule.getTypeDecls() : List<Decl.Type>
    = getPublicTypes().map { Decl.Type(it, emptyMap()) }

private fun IOrbModule.getOperatorDecls() : List<Decl.Operator>
    = getPublicOperators().map { Decl.Operator(it) }

fun IOrbModule.getPublicAPI() : Env
    = Env().extendAll(getTypeDecls()).extendAll(getOperatorDecls())