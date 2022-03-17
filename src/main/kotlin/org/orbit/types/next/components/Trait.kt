package org.orbit.types.next.components

data class Trait(override val fullyQualifiedName: String, val contracts: List<Contract> = emptyList(), override val isSynthetic: Boolean = false) : IType, Contract {
    override fun isImplemented(ctx: Ctx, by: IType): Boolean = when (by) {
        is Type -> StructuralEq.eq(ctx, this, by)
        else -> false
    }

    inline fun <reified C: Contract> getTypedContracts() : List<C> = contracts.filterIsInstance<C>()

    fun merge(ctx: Ctx, other: Trait) : Trait? {
        val nFieldContracts = mutableListOf<FieldContract>()
        for (f1 in getTypedContracts<FieldContract>()) {
            for (f2 in other.getTypedContracts<FieldContract>()) {
                if (f1.field.fullyQualifiedName == f2.field.fullyQualifiedName) {
                    if (AnyEq.eq(ctx, f1.field, f2.field)) {
                        // Name is the same, types are related
                        // We need the least specific of the two here
                        val min = TypeMinimum.calculate(ctx, f1.field, f2.field)
                            ?: return null

                        if (min === f1.field) {
                            nFieldContracts.add(f1)
                        } else {
                            nFieldContracts.add(f2)
                        }
                    } else {
                        // Conflict! Same name, unrelated types
                        return null
                    }
                } else {
                    nFieldContracts.add(f1)
                    nFieldContracts.add(f2)
                }
            }
        }

        return Trait(fullyQualifiedName + "_" + other.fullyQualifiedName, nFieldContracts, true)
    }

    override fun compare(ctx: Ctx, other: IType): TypeRelation = when (other) {
        is Trait -> when (NominalEq.eq(ctx, this, other)) {
            true -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }

        is Type -> when (StructuralEq.eq(ctx, this, other)) {
            true -> TypeRelation.Related(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }

        else -> TypeRelation.Unrelated(this, other)
    }
}