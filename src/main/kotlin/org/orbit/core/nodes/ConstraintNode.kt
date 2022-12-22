package org.orbit.core.nodes

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyMetaType
import org.orbit.backend.typesystem.components.IMutableTypeEnvironment
import org.orbit.backend.typesystem.components.IType
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

    object And : AttributeOperator { override val op: String = "&" }
    object Or : AttributeOperator { override val op: String = "|" }

    fun apply(left: IType.Attribute.IAttributeApplication, right: IType.Attribute.IAttributeApplication, env: IMutableTypeEnvironment) : AnyMetaType
        = left.invoke(env) + right.invoke(env)
}

interface IAttributeExpressionNode : INode

data class AttributeArrowNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val parameters: List<TypeIdentifierNode>,
    val constraint: IAttributeExpressionNode
) : INode {
    override fun getChildren(): List<INode>
        = parameters + constraint
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

data class AttributeInvocationNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val identifier: TypeIdentifierNode,
    val arguments: List<TypeExpressionNode>
) : IAttributeExpressionNode {
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
