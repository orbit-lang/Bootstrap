package org.orbit.backend.typesystem.components

data class AttributeInvocationExpression(val attribute: IAttribute, val args: List<AnyType>) :
    IAttributeExpression {
    override val id: String = ".${attribute.id}(${args.joinToString(", ")})"

    override fun evaluate(env: IMutableTypeEnvironment): AnyMetaType
        = attribute.invoke(env)

    override fun getCardinality(): ITypeCardinality
        = ITypeCardinality.Zero

    override fun getUnsolvedTypeVariables(): List<TypeVar>
        = attribute.getUnsolvedTypeVariables()

    override fun substitute(substitution: Substitution): AnyType
        = AttributeInvocationExpression(attribute.substitute(substitution) as IAttribute, args.substitute(substitution))
}