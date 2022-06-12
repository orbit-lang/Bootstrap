package org.orbit.types.next.components

interface Operator : DeclType {
    val relativeName: String
    val symbol: String
    val parameters: List<TypeComponent>
    val result: TypeComponent
}

data class InfixOperator(override val relativeName: String, override val symbol: String, val lhs: TypeComponent, val rhs: TypeComponent, override val result: TypeComponent) : Operator {
    override val fullyQualifiedName: String
        = "${lhs.fullyQualifiedName}::$relativeName::${rhs.fullyQualifiedName}::${result.fullyQualifiedName}"
    override val isSynthetic: Boolean = false
    override val kind: Kind = IntrinsicKinds.Operator

    override val parameters: List<TypeComponent> = listOf(lhs, rhs)

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation {
        TODO("Not yet implemented")
    }
}
