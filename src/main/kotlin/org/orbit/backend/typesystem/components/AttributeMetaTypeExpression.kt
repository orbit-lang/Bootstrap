package org.orbit.backend.typesystem.components

data class AttributeMetaTypeExpression(val metaType: AnyMetaType) : IAttributeExpression {
    override val id: String = metaType.id

    override fun evaluate(env: IMutableTypeEnvironment): AnyMetaType
        = metaType

    override fun getCardinality(): ITypeCardinality
        = ITypeCardinality.Zero

    override fun getUnsolvedTypeVariables(): List<TypeVar>
        = emptyList()

    override fun substitute(substitution: Substitution): AnyType
        = this
}