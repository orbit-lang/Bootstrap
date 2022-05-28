package org.orbit.types.next.components

import org.orbit.core.components.TokenType
import org.orbit.core.components.TokenTypes
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import kotlin.math.max

sealed interface Sort : TypeComponent {
    override val fullyQualifiedName: String get() {
        return javaClass.simpleName
    }

    override val isSynthetic: Boolean get() = false
    override val kind: org.orbit.types.next.components.Kind get() = IntrinsicKinds.NeverKind

    object Entity : Sort
    object Kind : Sort
    object Order : Sort
    object Augmentation : Sort

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other) {
        is Sort -> when (fullyQualifiedName) {
            other.fullyQualifiedName -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }

        else -> TypeRelation.Unrelated(this, other)
    }
}

interface Kind : TypeComponent {
    override val fullyQualifiedName: String get() = javaClass.simpleName
    override val isSynthetic: Boolean get() = false
    override val kind: Kind get() = this

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other) {
        is Kind -> when (other.fullyQualifiedName) {
            fullyQualifiedName -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }

        else -> TypeRelation.Unrelated(this, other)
    }
}

sealed interface IntrinsicKinds : Kind {
    object NeverKind : IntrinsicKinds
    object Context : IntrinsicKinds
    object Extension : IntrinsicKinds
    object Projection : IntrinsicKinds
    object Value : IntrinsicKinds
    object Type : IntrinsicKinds
    object Trait : IntrinsicKinds
    object Family : IntrinsicKinds
}

fun Kind.toString(printer: Printer) : String
    = printer.apply(fullyQualifiedName, PrintableKey.Bold, PrintableKey.Italics)

interface CompositeKind : Kind {
    val op: String
}

data class Product(val left: Kind, val right: Kind) : CompositeKind {
    override val fullyQualifiedName: String = "${left.fullyQualifiedName} + ${right.fullyQualifiedName}"
    override val isSynthetic: Boolean = true
    override val op: String = "+"
}

fun Product.toString(printer: Printer) : String {
    return "${left.toString(printer)} + ${right.toString(printer)}"
}

data class Sum(val left: Kind, val right: Kind) : CompositeKind {
    override val fullyQualifiedName: String = "${left.fullyQualifiedName} * ${right.fullyQualifiedName}"
    override val isSynthetic: Boolean = true
    override val op: String = "*"

    override val kind: Kind = this
}

data class TupleKind(val elements: List<Kind>) : Kind {
    override val fullyQualifiedName: String get() {
        return elements.joinToString(", ") { it.kind.fullyQualifiedName }
    }

    override val isSynthetic: Boolean = true
    override val kind: Kind = this
}

fun Sum.toString(printer: Printer) : String {
    return "${left.toString(printer)} * ${right.toString(printer)}"
}

data class HigherKind(val from: Kind, val to: Kind) : Kind {
    companion object {
        val typeConstructor1 = HigherKind(IntrinsicKinds.Type, IntrinsicKinds.Type)
    }

    override val fullyQualifiedName: String = "(${from.fullyQualifiedName}) -> (${to.fullyQualifiedName})"
    override val isSynthetic: Boolean = true
    override val kind: Kind = this
}