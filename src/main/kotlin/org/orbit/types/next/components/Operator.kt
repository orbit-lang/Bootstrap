package org.orbit.types.next.components

import org.orbit.core.nodes.OperatorFixity

interface Operator : DeclType {
    val relativeName: String
    val symbol: String
    val parameters: List<TypeComponent>
    val result: TypeComponent

    override val isSynthetic: Boolean
        get() = false

    override val kind: Kind
        get() = IntrinsicKinds.Operator

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation {
        TODO("Not yet implemented")
    }
}

data class InfixOperator(override val relativeName: String, override val symbol: String, val lhs: TypeComponent, val rhs: TypeComponent, override val result: TypeComponent) : Operator {
    override val fullyQualifiedName: String
        = "${lhs.fullyQualifiedName}::$relativeName::${rhs.fullyQualifiedName}::${result.fullyQualifiedName}"

    override val parameters: List<TypeComponent> = listOf(lhs, rhs)
}

data class UnaryOperator(val fixity: OperatorFixity, override val relativeName: String, override val symbol: String, val operand: TypeComponent, override val result: TypeComponent) : Operator {
    override val fullyQualifiedName: String get() = when (fixity) {
        OperatorFixity.Prefix -> "$relativeName::${operand.fullyQualifiedName}::${result.fullyQualifiedName}"
        OperatorFixity.Postfix -> "${operand.fullyQualifiedName}::${result.fullyQualifiedName}::$relativeName"
        else -> TODO("$relativeName is not a unary operator ($fixity)")
    }

    override val parameters: List<TypeComponent>
        = listOf(operand)
}
