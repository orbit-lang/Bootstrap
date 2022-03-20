package org.orbit.types.next.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.SourcePosition
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import java.lang.Exception

interface IType {
    val fullyQualifiedName: String
    val isSynthetic: Boolean

    fun compare(ctx: Ctx, other: IType) : TypeRelation
    fun inferenceKey() : String = fullyQualifiedName
}

sealed interface InternalControlType : IType

object Anything : InternalControlType {
    override val fullyQualifiedName: String = "*"
    override val isSynthetic: Boolean = true

    override fun compare(ctx: Ctx, other: IType): TypeRelation = TypeRelation.Unrelated(this, other)
}

interface NeverType : InternalControlType {
    val message: String
    val position: SourcePosition
}

data class Never(override val message: String = "", override val position: SourcePosition = SourcePosition.unknown) : Exception(message), NeverType, ExecutableType<NeverType>, KoinComponent {
    companion object : NeverType {
        override val fullyQualifiedName: String = "_"
        override val isSynthetic: Boolean = true
        override val position: SourcePosition = SourcePosition.unknown
        override val message: String = "Encountered Never type"

        override fun compare(ctx: Ctx, other: IType): TypeRelation
                = TypeRelation.Unrelated(this, other)
    }

    private val invocation: Invocation by inject()

    override val fullyQualifiedName: String = "_"
    override val isSynthetic: Boolean = true

    override val takes: NeverType = Never
    override val returns: IType = Never

    override fun compare(ctx: Ctx, other: IType): TypeRelation = TypeRelation.Unrelated(this, other)
}

interface DeclType : IType
interface ValueType : IType

fun IType.toString(printer: Printer) : String
    = printer.apply(fullyQualifiedName, PrintableKey.Bold)

interface VectorType : ValueType, Collection<IType> {
    val elements: List<IType>

    override val isSynthetic: Boolean
        get() = true

    fun nth(n: Int) : IType

    override fun compare(ctx: Ctx, other: IType): TypeRelation = when (other) {
        is VectorType -> when (other.elements.count()) {
            elements.count() -> {
                val allSame = elements.zip(other.elements).all {
                    it.first.compare(ctx, it.second) is TypeRelation.Same
                }

                when (allSame) {
                    true -> TypeRelation.Same(this, other)
                    else -> TypeRelation.Unrelated(this, other)
                }
            }
            else -> TypeRelation.Unrelated(this, other)
        }
        else -> TypeRelation.Unrelated(this, other)
    }
}

data class PairType(val first: IType, val second: IType) : VectorType, Collection<IType> by listOf(first, second) {
    override val fullyQualifiedName: String = "(0: ${first.fullyQualifiedName}, 1: ${second.fullyQualifiedName})"
    override val elements: List<IType> = listOf(first, second)

    override fun nth(n: Int): IType = when (n) {
        0 -> first
        1 -> second
        else -> Never("Pair index out of bounds: $fullyQualifiedName[$n]")
    }
}

data class TupleType(override val elements: List<IType>) : VectorType, Collection<IType> by elements {
    override val fullyQualifiedName: String get() {
        val elems = elements.mapIndexed { idx, t ->
            "$idx: ${t.fullyQualifiedName}"
        }.joinToString(", ")

        return "($elems)"
    }

    override fun nth(n: Int) : IType = when {
        n > -1 && n < elements.count() -> elements[n]
        else -> Never("Tuple index out of bounds: ${fullyQualifiedName}[$n]")
    }
}

data class ListType(override val elements: List<IType>) : VectorType, Collection<IType> by elements {
    override val fullyQualifiedName: String = "(${elements.join()})"

    override fun nth(n: Int): IType = when {
        n > -1 && n < elements.count() -> elements[n]
        else -> Never("List index out of bounds: ${fullyQualifiedName}[$n]")
    }
}
