package org.orbit.types.next.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.SourcePosition
import org.orbit.util.Invocation
import org.orbit.util.Printer

interface TypeComponent {
    val fullyQualifiedName: String
    val isSynthetic: Boolean
    val kind: Kind

    fun compare(ctx: Ctx, other: TypeComponent) : TypeRelation
    fun inferenceKey() : String = fullyQualifiedName
    fun references(type: TypeComponent) : Boolean = type.fullyQualifiedName == fullyQualifiedName

    fun toString(printer: Printer) : String
        = printer.apply(fullyQualifiedName, org.orbit.util.PrintableKey.Bold)
}

interface Substitutor<T: TypeComponent> {
    fun substitute(target: T, old: TypeComponent, new: TypeComponent) : T
}

object TypeComponentSubstitutor : Substitutor<TypeComponent> {
    override fun substitute(target: TypeComponent, old: TypeComponent, new: TypeComponent): TypeComponent = when (target.fullyQualifiedName) {
        old.fullyQualifiedName -> new
        else -> target
    }
}

fun <T: TypeComponent> T.substitute(old: TypeComponent, new: TypeComponent, using: Substitutor<T> = TypeComponentSubstitutor as Substitutor<T>) : T
    = using.substitute(this, old, new)

fun TypeComponent.resolve(ctx: Ctx) : TypeComponent?
    = ctx.getType(fullyQualifiedName)

data class Mirror(val reflectedType: TypeComponent) : TypeComponent {
    override val fullyQualifiedName: String = reflectedType.fullyQualifiedName
    override val isSynthetic: Boolean = true
    override val kind: Kind = IntrinsicKinds.Type

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other) {
        is Mirror -> reflectedType.compare(ctx, other.reflectedType)
        else -> reflectedType.compare(ctx, other)
    }
}

data class Never(override val message: String = "", override val position: SourcePosition = SourcePosition.unknown) : Exception(message), NeverType, ExecutableType<NeverType>, KoinComponent {
    companion object : NeverType {
        override val fullyQualifiedName: String = "_"
        override val isSynthetic: Boolean = true
        override val position: SourcePosition = SourcePosition.unknown
        override val message: String = "Encountered Never type"
        override val target: TypeComponent = this

        override val trait: ITrait = this
        override val input: ITrait = this

        override fun deriveTrait(ctx: Ctx): ITrait = Never

        override fun isImplemented(ctx: Ctx, by: TypeComponent): ContractResult
                = ContractResult.Failure(by, this)

        override fun getErrorMessage(printer: Printer, type: TypeComponent): String {
            TODO("Not yet implemented")
        }

        override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation
                = TypeRelation.Unrelated(this, other)

        override fun plus(other: InternalControlType): InternalControlType = when (other) {
            is Anything -> this
            else -> other
        }

        override fun substitute(old: TypeComponent, new: TypeComponent): Contract<ITrait> = this
    }

    private val invocation: Invocation by inject()

    override val fullyQualifiedName: String = "_"
    override val isSynthetic: Boolean = true

    override val takes: NeverType = Never
    override val returns: TypeComponent = Never
    override val trait: ITrait = this
    override val input: ITrait = this
    override val target: TypeComponent = this

    override fun deriveTrait(ctx: Ctx): ITrait = Never

    override fun isImplemented(ctx: Ctx, by: TypeComponent): ContractResult
            = ContractResult.Failure(by, this)

    override fun getErrorMessage(printer: Printer, type: TypeComponent): String {
        TODO("Not yet implemented")
    }

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = TypeRelation.Unrelated(this, other)

    override fun plus(other: InternalControlType): InternalControlType = when (other) {
        is Never -> Never(message + "\n" + other.message)
        else -> this
    }

    override fun substitute(old: TypeComponent, new: TypeComponent): Contract<ITrait> = this
}

fun List<Never>.combine(message: String) : Never {
    val pretty = joinToString("\n\t") { it.message }

    return Never(message + "\n\t$pretty")
}

interface DeclType : TypeComponent {
    fun isWeakenedBy(other: DeclType) : Boolean = false
}

interface ValueType : TypeComponent

interface VectorType : ValueType, Collection<TypeComponent> {
    val elements: List<TypeComponent>

    override val kind: Kind get() = IntrinsicKinds.Type

    override val isSynthetic: Boolean
        get() = true

    fun nth(n: Int) : TypeComponent

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other) {
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

data class PairType(val first: TypeComponent, val second: TypeComponent) : VectorType, Collection<TypeComponent> by listOf(first, second) {
    override val fullyQualifiedName: String = "(0: ${first.fullyQualifiedName}, 1: ${second.fullyQualifiedName})"
    override val elements: List<TypeComponent> = listOf(first, second)

    override fun nth(n: Int): TypeComponent = when (n) {
        0 -> first
        1 -> second
        else -> Never("Pair index out of bounds: $fullyQualifiedName[$n]")
    }
}

data class TupleType(override val elements: List<TypeComponent>) : VectorType, Collection<TypeComponent> by elements {
    override val fullyQualifiedName: String get() {
        val elems = elements.mapIndexed { idx, t ->
            "$idx: ${t.fullyQualifiedName}"
        }.joinToString(", ")

        return "($elems)"
    }

    override fun nth(n: Int) : TypeComponent = when {
        n > -1 && n < elements.count() -> elements[n]
        else -> Never("Tuple index out of bounds: ${fullyQualifiedName}[$n]")
    }
}

data class ListType(override val elements: List<TypeComponent>) : VectorType, Collection<TypeComponent> by elements {
    override val fullyQualifiedName: String = "(${elements.join()})"

    override fun nth(n: Int): TypeComponent = when {
        n > -1 && n < elements.count() -> elements[n]
        else -> Never("List index out of bounds: ${fullyQualifiedName}[$n]")
    }
}