package org.orbit.types.next.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.util.Printer

interface ITrait : Entity, Contract<ITrait>

data class Trait(override val fullyQualifiedName: String, val contracts: List<Contract<*>> = emptyList(), override val isSynthetic: Boolean = false) : ITrait {
    constructor(path: Path, contracts: List<Contract<*>> = emptyList(), isSynthetic: Boolean = false) : this(path.toString(OrbitMangler), contracts, isSynthetic)

    override val trait: Trait = this
    override val input: Trait = this

    override fun isImplemented(ctx: Ctx, by: TypeComponent): ContractResult {
        val start: ContractResult = ContractResult.None

        return contracts.fold(start) { acc, next ->
            acc + (next.isImplemented(ctx, by))
        }
    }

    override fun getErrorMessage(printer: Printer, type: TypeComponent): String
        = "Type ${type.toString(printer)} does not implement Trait ${toString(printer)}"

    inline fun <reified C: Contract<*>> getTypedContracts() : List<C> = contracts.filterIsInstance<C>()

    fun merge(ctx: Ctx, other: Trait) : Trait? {
        val nFieldContracts = mutableListOf<FieldContract>()
        for (f1 in getTypedContracts<FieldContract>()) {
            for (f2 in other.getTypedContracts<FieldContract>()) {
                if (f1.input.fullyQualifiedName == f2.input.fullyQualifiedName) {
                    if (AnyEq.eq(ctx, f1.input, f2.input)) {
                        // Name is the same, types are related
                        // We need the least specific of the two here
                        val min = TypeMinimum.calculate(ctx, f1.input, f2.input)
                            ?: return null

                        if (min === f1.input) {
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

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other) {
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