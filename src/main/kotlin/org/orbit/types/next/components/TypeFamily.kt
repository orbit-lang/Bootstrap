package org.orbit.types.next.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.util.Printer

data class TypeFamily<T: TypeComponent>(override val fullyQualifiedName: String, val members: List<T>) : ITrait {
    constructor(fullyQualifiedName: String, vararg members: T) : this(fullyQualifiedName, members.toList())
    constructor(path: Path, vararg members: T) : this(path.toString(OrbitMangler), members.toList())
    constructor(path: Path, members: List<T>) : this(path.toString(OrbitMangler), members)

    override val trait: ITrait = Trait(fullyQualifiedName)
    override val input: ITrait = Trait(fullyQualifiedName)
    override val contracts: List<Contract<*>> = emptyList()

    override val isSynthetic: Boolean = false
    override val kind: Kind = IntrinsicKinds.Family

    override fun merge(ctx: Ctx, other: ITrait): ITrait = other

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other) {
        is TypeFamily<*> -> when (fullyQualifiedName == other.fullyQualifiedName) {
            true -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }

        else -> when (members.anyEq(ctx, other)) {
            true -> TypeRelation.Member(this, other as T)
            else -> TypeRelation.Unrelated(this, other)
        }
    }

    override fun isImplemented(ctx: Ctx, by: TypeComponent): ContractResult = when (members.anyEq(ctx, by)) {
        true -> ContractResult.Success(by, this)
        else -> ContractResult.Failure(by, this)
    }

    override fun getErrorMessage(printer: Printer, type: TypeComponent): String
        = "${type.toString(printer)} is not a member of Type Family ${this.toString(printer)}"
}