package org.orbit.types.next.components

import org.orbit.core.components.TokenType
import org.orbit.core.components.TokenTypes
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import kotlin.math.max

interface Kind : TypeComponent {
    override val fullyQualifiedName: String
        get() = keyword.identifier

    val keyword: TokenType
    val level: Int

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other) {
        is Kind -> when (other.fullyQualifiedName) {
            fullyQualifiedName -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }

        else -> TypeRelation.Unrelated(this, other)
    }
}

sealed interface IntrinsicKinds : Kind {
    object Context : IntrinsicKinds {
        override val level: Int = Int.MAX_VALUE
        override val keyword: TokenType = TokenTypes.Context
    }

    object Value : IntrinsicKinds {
        override val level: Int = -1
        override val keyword: TokenType
            = object : TokenType("Value", "Value", true, false, Family.Kind) {}
    }

    data class Type(override val level: Int = 0) : IntrinsicKinds {
        override val keyword: TokenType = TokenTypes.Type

        companion object : IntrinsicKinds {
            override val keyword: TokenType = TokenTypes.Type
            override val level: Int = 0
        }
    }

    data class Trait(override val level: Int = 0) : IntrinsicKinds {
        override val keyword: TokenType = TokenTypes.Trait

        companion object : IntrinsicKinds {
            override val keyword: TokenType = TokenTypes.Trait
            override val level: Int = 0
        }
    }

    data class Family<K: Kind>(override val kind: K) : IntrinsicKinds {
        override val keyword: TokenType = TokenTypes.Family
        override val level: Int = kind.level
    }

    override val kind: Kind get() = when (this) {
        is Type, Type -> Type(level + 1)
        is Trait, Trait -> Trait(level + 1)
        else -> this
    }

    companion object {
        fun values() : List<IntrinsicKinds> = listOf(Type, Trait)

        fun valueOf(keyword: TokenType) : Kind?
            = values().firstOrNull { it.keyword == keyword }
    }

    override val isSynthetic: Boolean get() = false
}

fun Kind.toString(printer: Printer) : String
    = printer.apply(fullyQualifiedName, PrintableKey.Bold, PrintableKey.Italics)

interface CompositeKind : Kind {
    val op: String
}

data class Product(val left: Kind, val right: Kind) : CompositeKind {
    override val fullyQualifiedName: String = "${left.fullyQualifiedName} + ${right.fullyQualifiedName}"
    override val level: Int = max(left.level, right.level)
    override val isSynthetic: Boolean = true
    override val op: String = "+"

    override val kind: Kind = this
    override val keyword: TokenType = TokenTypes.Type
}

fun Product.toString(printer: Printer) : String {
    return "${left.toString(printer)} + ${right.toString(printer)}"
}

data class Sum(val left: Kind, val right: Kind) : CompositeKind {
    override val fullyQualifiedName: String = "${left.fullyQualifiedName} * ${right.fullyQualifiedName}"
    override val level: Int = max(left.level, right.level)
    override val isSynthetic: Boolean = true
    override val op: String = "*"

    override val kind: Kind = this
    override val keyword: TokenType = TokenTypes.Type
}

data class TupleKind(val elements: List<Kind>) : Kind {
    override val fullyQualifiedName: String get() {
        return elements.joinToString(", ") { it.kind.fullyQualifiedName }
    }

    override val level: Int = elements.maxOf { it.level }
    override val isSynthetic: Boolean = true

    override val kind: Kind = this
    override val keyword: TokenType = TokenTypes.Type
}

fun Sum.toString(printer: Printer) : String {
    return "${left.toString(printer)} * ${right.toString(printer)}"
}

data class HigherKind(val from: Kind, val to: Kind) : Kind {
    override val fullyQualifiedName: String = "(${from.fullyQualifiedName}) -> (${to.fullyQualifiedName})"
    override val level: Int = from.level + 1
    override val isSynthetic: Boolean = true

    override val kind: Kind = this

    override val keyword: TokenType
        get() = TokenTypes.HigherKind(level)
}