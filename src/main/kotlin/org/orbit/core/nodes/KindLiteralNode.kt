package org.orbit.core.nodes

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.SourcePosition
import org.orbit.core.components.Token
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

sealed interface KindLiteral {
    val symbol: String

    sealed interface Entity : KindLiteral {
        companion object : KoinComponent {
            private val invocation: Invocation by inject()

            fun valueOf(symbol: String) : Entity = when (symbol) {
                TokenTypes.Type.pattern -> Type
                TokenTypes.Trait.pattern -> Trait
                else -> throw invocation.make<Parser>("`$symbol` is not an Entity Kind literal", SourcePosition.unknown)
            }

            fun valueOf(token: Token) : Entity
                = valueOf(token.text)
        }

        object Type : Entity {
            override val symbol: String = "type"
        }

        object Trait : Entity {
            override val symbol: String = "trait"
        }
    }

    data class Set(val elements: List<KindLiteral>) : KindLiteral {
        override val symbol: String = "(${elements.joinToString(", ") { it.symbol }}"
    }

    data class Constructor(val left: Set, val right: Entity) : KindLiteral {
        override val symbol: String = "(${left.symbol} -> ${right.symbol})"
    }
}

data class KindLiteralNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val kind: KindLiteral
) : INode {
    override fun getChildren(): List<INode> = emptyList()
}
