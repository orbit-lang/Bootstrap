package org.orbit.types.next.components

private fun IType.tryCurry() : IType = when (this) {
    is Func -> curry()
    else -> this
}

fun List<IType>.join() : String
    = joinToString(", ") { it.fullyQualifiedName }

data class Func(override val takes: VectorType, override val returns: IType) : ExecutableType<VectorType> {
    constructor(takes: List<IType>, returns: IType) : this(ListType(takes), returns)

    override val fullyQualifiedName: String
        = "${takes.fullyQualifiedName} -> ${returns.fullyQualifiedName}"

    override val isSynthetic: Boolean = false

    /*
        f : (A, B) -> C
        g = f(a, _) : (A) -> (B) -> C
        h = f(_, b) : (B) -> (A) -> C

        f : (A, B, C) -> D
        g = f(a, _, _) -> (B, C) -> D
        h = f(_, b, c) -> (A) -> D
        i = f(_, _, c) -> (A, B) -> D
     */
    fun partial(args: List<Pair<Int, IType>>) : ExecutableType<*> = when {
        args.isEmpty() -> this
        args.count() > takes.count() -> Never("Cannot partially apply executable type $fullyQualifiedName with more arguments than it declares (${args.count()} vs ${takes.count()})")
        else -> {
            val givenIndices = args.map(Pair<Int, IType>::first)
            val missing = takes.filterIndexed { idx, _ -> !givenIndices.contains(idx) }

            Func(missing, returns)
        }
    }

    fun curry() : Lambda = when (takes.count()) {
        0 -> Lambda(Never, returns.tryCurry())
        1 -> Lambda(takes.nth(0).tryCurry(), returns.tryCurry())
        else -> Lambda(takes.nth(0).tryCurry(), Func(takes.drop(1).map(IType::tryCurry), returns.tryCurry()).curry())
    }

    override fun compare(ctx: Ctx, other: IType): TypeRelation = when (other) {
        is Func -> curry().compare(ctx, other.curry())
        is Lambda -> curry().compare(ctx, other)
        else -> TypeRelation.Unrelated(this, other)
    }
}