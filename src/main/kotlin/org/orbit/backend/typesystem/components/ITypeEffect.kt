package org.orbit.backend.typesystem.components

sealed interface ITypeEffect : IType {
    fun invoke(env: IMutableTypeEnvironment) : AnyMetaType
}