package org.orbit.backend.typesystem.intrinsics

import org.orbit.backend.typesystem.components.IMutableTypeEnvironment
import org.orbit.backend.typesystem.components.IType

interface IOrbModule {
    fun getPublicTypes() : List<IType.Type>
    fun getPublicTypeAliases() : List<IType.Alias>
    fun getPublicOperators() : List<IType.IOperatorArrow<*, *>>
}

fun IMutableTypeEnvironment.import(module: IOrbModule) : IMutableTypeEnvironment = this.apply {
    module.getPublicTypes().forEach(::add)
    module.getPublicTypeAliases().forEach(::add)
    module.getPublicOperators().forEach(::add)
}