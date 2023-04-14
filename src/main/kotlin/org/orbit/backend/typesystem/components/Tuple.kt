package org.orbit.backend.typesystem.components

data class Tuple(val left: AnyType, val right: AnyType) : IProductType<Int, Tuple>, ICaseIterable<Tuple> {
    override val id: String = "(${left.id} * ${right.id})"

    override fun getUnsolvedTypeVariables(): List<TypeVar>
        = left.getUnsolvedTypeVariables() + right.getUnsolvedTypeVariables()

    override fun isSpecialised(): Boolean = when (left) {
        is ISpecialisedType -> left.isSpecialised()
        else -> when (right) {
            is ISpecialisedType -> right.isSpecialised()
            else -> false
        }
    }

    override fun getCases(result: AnyType): List<Case> {
        val leftCases = when (left) {
            is ICaseIterable<*> -> left.getCases(result)
            else -> listOf(Case(left, result))
        }

        val rightCases = when (right) {
            is ICaseIterable<*> -> right.getCases(result)
            else -> listOf(Case(right, result))
        }

        val cases = mutableListOf<Case>()
        for (lCase in leftCases) {
            for (rCase in rightCases) {
                val nCase = Case(Tuple(lCase.condition, rCase.condition), result)
                val allCases = cases.map { it.id }

                if (!allCases.contains(nCase.id)) cases.add(nCase)
            }
        }

        return cases
    }

    private fun getLeftConstructors() : List<IConstructor<*>> = when (left) {
        is IConstructableType<*> -> left.getConstructors()
        else -> emptyList()
    }

    private fun getRightConstructors() : List<IConstructor<*>> = when (right) {
        is IConstructableType<*> -> right.getConstructors()
        else -> emptyList()
    }

    override fun getConstructors(): List<IConstructor<Tuple>> {
        val constructors = mutableListOf<TupleConstructor>()
        for (lConstructor in getLeftConstructors()) {
            for (rConstructor in getRightConstructors()) {
                var lDomain = lConstructor.getDomain()
                var rDomain = rConstructor.getDomain()

                if (lDomain.count() > 1 || rDomain.count() > 1) TODO("2+-ary Tuple Constructors")

                if (lDomain.isEmpty() && lConstructor is SingletonConstructor) {
                    lDomain = listOf(lConstructor.getCodomain())
                }

                if (rDomain.isEmpty() && rConstructor is SingletonConstructor) {
                    rDomain = listOf(rConstructor.getCodomain())
                }

                val constructor = TupleConstructor(lDomain[0], rDomain[0], this)

                if (constructors.none { it.id == constructor.id }) {
                    constructors.add(constructor)
                }
            }
        }

        return constructors
    }

    override fun getCardinality(): ITypeCardinality
        = left.getCardinality() + right.getCardinality()

    override fun getElement(at: Int): AnyType = when (at) {
        0 -> left
        1 -> right
        else -> Never("Attempt to retrieve element from Tuple at index $at")
    }

    override fun substitute(substitution: Substitution): Tuple
        = Tuple(left.substitute(substitution), right.substitute(substitution))

    override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType
        = Tuple(left.flatten(from, env), right.flatten(from, env))

    override fun prettyPrint(depth: Int): String
        = "($left, $right)"

    override fun toString(): String = prettyPrint()
}