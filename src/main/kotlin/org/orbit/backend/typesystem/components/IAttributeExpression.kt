package org.orbit.backend.typesystem.components

sealed interface IAttributeExpression : IType {
    fun evaluate(env: IMutableTypeEnvironment) : AnyMetaType
}