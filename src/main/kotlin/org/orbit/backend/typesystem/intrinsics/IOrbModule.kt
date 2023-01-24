package org.orbit.backend.typesystem.intrinsics

import org.orbit.backend.typesystem.components.*

interface IOrbModule {
    fun getPublicTypes() : List<AnyType>
    fun getPublicTypeAliases() : List<IType.Alias>
    fun getPublicOperators() : List<IType.IOperatorArrow<*, *>>
    fun getContexts() : List<Context> = emptyList()
}

fun IMutableTypeEnvironment.import(module: IOrbModule) : IMutableTypeEnvironment = this.apply {
    module.getPublicTypes().forEach(::add)
    module.getPublicTypeAliases().forEach(::add)
    module.getPublicOperators().forEach(::add)
    module.getContexts().forEach(::add)
}