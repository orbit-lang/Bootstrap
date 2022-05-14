package org.orbit.types.next.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.components.SourcePosition
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

interface ITrait : Entity, Contract<ITrait> {
    val contracts: List<Contract<*>>

    fun merge(ctx: Ctx, other: ITrait) : ITrait
    fun <C: Contract<*>> getTypedContracts(clazz: Class<C>) : List<C> = contracts.filterIsInstance(clazz)
}

fun List<ITrait>.mergeAll(ctx: Ctx) : ITrait = fold(Anything as ITrait) { acc, next ->
    when (val r = acc.merge(ctx, next)) {
        null -> acc
        else -> r
    }
}

data class Trait(override val fullyQualifiedName: String, override val contracts: List<Contract<*>> = emptyList(), override val isSynthetic: Boolean = false) : ITrait, FieldAwareType {
    constructor(path: Path, contracts: List<Contract<*>> = emptyList(), isSynthetic: Boolean = false) : this(path.toString(OrbitMangler), contracts, isSynthetic)

    override val trait: Trait = this
    override val input: Trait = this

    override val kind: Kind = IntrinsicKinds.Trait

    override fun isImplemented(ctx: Ctx, by: TypeComponent): ContractResult = when (StructuralEq.eq(ctx, this, by)) {
        true -> ContractResult.Success(by, this)
        else -> ContractResult.Failure(by, this)
    }

    override fun getFields(): List<Field> = contracts.filterIsInstance<FieldContract>()
        .map { it.input }

    override fun getErrorMessage(printer: Printer, type: TypeComponent): String
        = "Type ${type.toString(printer)} does not implement Trait ${toString(printer)}"

    inline fun <reified C: Contract<*>> getTypedContracts() : List<C> = contracts.filterIsInstance<C>()

    override fun merge(ctx: Ctx, other: ITrait) : ITrait {
        val printer: Printer = getKoinInstance()

        val nFieldContracts = mutableListOf<FieldContract>()
        for (f1 in getTypedContracts<FieldContract>()) {
            for (f2 in other.getTypedContracts(FieldContract::class.java)) {
                if (f1.input.name == f2.input.name) {
                    if (AnyEq.eq(ctx, f1.input, f2.input)) {
                        // Name is the same, types are related
                        // We need the least specific of the two here
                        val min = TypeMinimum.calculate(ctx, f1.input, f2.input)
                            ?: return Never("Cannot merge Trait fields ${f1.input.toString(printer)} & ${f2.input.toString(printer)}")

                        if (min === f1.input) {
                            nFieldContracts.add(f1)
                        } else {
                            nFieldContracts.add(f2)
                        }
                    } else {
                        // Conflict! Same name, unrelated types
                        val invocation: Invocation = getKoinInstance()
                        val printer: Printer = getKoinInstance()

                        throw invocation.make<TypeSystem>("Conflict found between Fields ${f1.input.toString(printer)} & ${f2.input.toString(printer)}", SourcePosition.unknown)
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

    override fun equals(other: Any?): Boolean = when (other) {
        is Trait -> fullyQualifiedName == other.fullyQualifiedName
        else -> false
    }
}