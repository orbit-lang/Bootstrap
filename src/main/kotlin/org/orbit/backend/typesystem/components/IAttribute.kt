package org.orbit.backend.typesystem.components

sealed interface IAttribute : IType {
    fun invoke(env: IMutableTypeEnvironment) : AnyMetaType
}