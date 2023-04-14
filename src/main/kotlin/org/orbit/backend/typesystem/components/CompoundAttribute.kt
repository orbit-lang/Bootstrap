package org.orbit.backend.typesystem.components

import org.orbit.core.nodes.AttributeOperator

data class CompoundAttribute(val op: AttributeOperator, val left: IAttribute, val right: IAttribute) : IAttribute {
    override val id: String = "${left.id} $op ${right.id}"

    override fun invoke(env: IMutableTypeEnvironment): AnyMetaType
        = op.apply(left, right, env)

    override fun getCardinality(): ITypeCardinality
        = ITypeCardinality.Zero

    override fun getUnsolvedTypeVariables(): List<TypeVar>
        = left.getUnsolvedTypeVariables() + right.getUnsolvedTypeVariables()

    override fun substitute(substitution: Substitution): AnyType
        =
        CompoundAttribute(op, left.substitute(substitution) as IAttribute, right.substitute(substitution) as IAttribute)

    override fun prettyPrint(depth: Int): String
        = "($left $op $right)"

    override fun toString(): String
        = prettyPrint()
}