package org.orbit.types.next.components

import org.orbit.core.components.TokenType
import org.orbit.frontend.components.TokenTypes

interface Kind : TypeComponent {
    override val fullyQualifiedName: String
        get() = keyword.identifier

    val keyword: TokenType

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other) {
        is Kind -> when (other.fullyQualifiedName) {
            fullyQualifiedName -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }

        else -> TypeRelation.Unrelated(this, other)
    }
}

enum class IntrinsicKinds(override val keyword: TokenType) : Kind {
    Type(TokenTypes.Type), Trait(TokenTypes.Trait);

    companion object {
        fun valueOf(keyword: TokenType) : Kind?
            = values().firstOrNull { it.keyword == keyword }
    }

    override val isSynthetic: Boolean = false
}