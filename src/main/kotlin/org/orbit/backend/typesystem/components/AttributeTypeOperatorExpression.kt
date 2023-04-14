package org.orbit.backend.typesystem.components

import org.orbit.core.nodes.ITypeBoundsOperator

data class AttributeTypeOperatorExpression(val op: ITypeBoundsOperator, val left: AnyType, val right: AnyType) :
    IAttributeExpression {
    override val id: String = "(${left.id} $op ${right.id})"

    override fun evaluate(env: IMutableTypeEnvironment): AnyMetaType
        = op.apply(left, right, env)

    override fun getCardinality(): ITypeCardinality
        = ITypeCardinality.Zero

    override fun getUnsolvedTypeVariables(): List<TypeVar>
        = (left.getUnsolvedTypeVariables() + right.getUnsolvedTypeVariables()).distinct()

    override fun substitute(substitution: Substitution): AnyType
        = AttributeTypeOperatorExpression(op, left.substitute(substitution), right.substitute(substitution))

    override fun prettyPrint(depth: Int): String {
        val indent = "\t".repeat(depth)

        return "$indent$left $op $right"
    }

    override fun toString(): String
        = prettyPrint()
}