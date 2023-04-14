package org.orbit.core.nodes

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.core.components.Token
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

sealed interface AttributeOperator {
    companion object : KoinComponent {
        private val invocation: Invocation by inject()

        fun valueOf(token: Token) : AttributeOperator = when (token.text) {
            "&" -> And
            "|" -> Or
            else -> throw invocation.make<Parser>("Illegal Attribute operator `${token.text}`", token)
        }
    }

    val op: String

    object And : AttributeOperator {
        override val op: String = "&"

        override fun apply(left: IAttribute, right: IAttribute, env: IMutableTypeEnvironment): AnyMetaType
            = left.invoke(env) + right.invoke(env)

        override fun toString(): String = op
    }

    object Or : AttributeOperator {
        override val op: String = "|"

        override fun apply(left: IAttribute, right: IAttribute, env: IMutableTypeEnvironment): AnyMetaType = when (val l = left.invoke(env)) {
            Always -> l
            else -> when (val r = right.invoke(env)) {
                is Always -> Always
                else -> r
            }
        }

        override fun toString(): String = op
    }

    fun apply(left: IAttribute, right: IAttribute, env: IMutableTypeEnvironment) : AnyMetaType
}

sealed interface IAttributeExpressionNode : INode

data class AttributeArrowNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val parameters: List<TypeIdentifierNode>,
    val constraint: IAttributeExpressionNode,
    val effects: List<TypeEffectInvocationNode>
) : INode {
    override fun getChildren(): List<INode>
        = parameters + constraint + effects
}

data class CompoundAttributeExpressionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val op: AttributeOperator,
    val leftExpression: IAttributeExpressionNode,
    val rightExpression: IAttributeExpressionNode
) : IAttributeExpressionNode {
    override fun getChildren(): List<INode>
        = listOf(leftExpression, rightExpression)
}

data class AttributeOperatorExpressionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val op: ITypeBoundsOperator,
    val leftExpression: TypeExpressionNode,
    val rightExpression: TypeExpressionNode
) : IAttributeExpressionNode {
    override fun getChildren(): List<INode>
        = listOf(leftExpression, rightExpression)
}

data class AttributeMetaTypeExpressionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val metaType: TypeIdentifierNode
) : IAttributeExpressionNode {
    override fun getChildren(): List<INode>
        = listOf(metaType)
}

data class AttributeInvocationNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val identifier: TypeIdentifierNode,
    val arguments: List<TypeExpressionNode>
) : IAttributeExpressionNode, IContextClauseExpressionNode {
    override fun getChildren(): List<INode>
        = listOf(identifier) + arguments
}

data class AttributeDefNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val identifier: TypeIdentifierNode,
    val arrow: AttributeArrowNode
) : TopLevelDeclarationNode {
    override val context: IContextExpressionNode? = null

    override fun getChildren(): List<INode>
        = listOf(identifier, arrow)
}
