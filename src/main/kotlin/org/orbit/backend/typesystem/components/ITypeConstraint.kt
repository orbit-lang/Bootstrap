package org.orbit.backend.typesystem.components

sealed interface ITypeConstraint : IType {
    val type: AnyType

    fun isSolvedBy(input: AnyType, env: ITypeEnvironment) : Boolean

    override fun getCardinality(): ITypeCardinality {
        TODO("Not yet implemented")
    }
}

operator fun ITypeConstraint.plus(other: ITypeConstraint) : CompoundConstraint = when (this) {
    is CompoundConstraint -> when (other) {
        is CompoundConstraint -> CompoundConstraint(type, constraints + other.constraints)
        else -> CompoundConstraint(type, constraints + other)
    }

    else -> when (other) {
        is CompoundConstraint -> CompoundConstraint(type, other.constraints + this)
        else -> CompoundConstraint(type, listOf(this, other))
    }
}

data class ConformanceConstraint(override val type: AnyType, val trait: IType.Trait) : ITypeConstraint {
    override val id: String = "${type.id} : ${trait.id}"

    override fun isSolvedBy(input: AnyType, env: ITypeEnvironment): Boolean
        = trait.isImplementedBy(input, env)

    override fun substitute(substitution: Substitution): AnyType
        = ConformanceConstraint(type.substitute(substitution), trait.substitute(substitution))

    override fun prettyPrint(depth: Int): String {
        val indent = "\t".repeat(depth)

        return "$indent`$type : $trait`"
    }

    override fun toString(): String
        = prettyPrint(0)
}

data class CompoundConstraint(override val type: AnyType, val constraints: List<ITypeConstraint>) : ITypeConstraint {
    override val id: String = "${type.id} : ${constraints.map { it.id }}"

    override fun isSolvedBy(input: AnyType, env: ITypeEnvironment): Boolean
        = constraints.all { it.isSolvedBy(input, env) }

    override fun substitute(substitution: Substitution): AnyType {
        TODO("Not yet implemented")
    }
}
