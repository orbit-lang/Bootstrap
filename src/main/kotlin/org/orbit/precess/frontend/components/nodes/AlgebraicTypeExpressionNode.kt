package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.Expr
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.components.TypeOperator
import org.orbit.precess.backend.utils.AnyType
import org.orbit.precess.backend.utils.TypeUtils

abstract class AlgebraicTypeExpressionNode(open val left: TermExpressionNode<*>, open val right: TermExpressionNode<*>, open val op: TypeOperator) : TermExpressionNode<Expr.AnyTypeLiteral>() {
    override fun getChildren(): List<Node> = listOf(left, right)
    override fun toString(): String = "($left ${op.symbol} $right)"

    abstract fun infer(leftType: AnyType, rightType: AnyType) : AnyType

    override fun getExpression(env: Env): Expr.AnyTypeLiteral {
        val leftType = left.getExpression(env).infer(env)
        val rightType = right.getExpression(env).infer(env)
        val algebraicType = infer(leftType, rightType)

        return Expr.AnyTypeLiteral(algebraicType)
    }

//    override fun infer(env: Env): AnyType {
//        val leftType = left.getExpression(env).infer(env)
//        val rightType = right.getExpression(env).infer(env)
//
//        return infer(leftType, rightType)
//    }
}

data class ProductTypeExpressionNode(override val firstToken: Token, override val lastToken: Token, override val left: TermExpressionNode<*>, override val right: TermExpressionNode<*>) : AlgebraicTypeExpressionNode(left, right, TypeOperator.Product) {
    override fun infer(leftType: AnyType, rightType: AnyType): IType.Tuple
        = IType.Tuple(leftType, rightType)
}

data class SumTypeExpressionNode(override val firstToken: Token, override val lastToken: Token, override val left: TermExpressionNode<*>, override val right: TermExpressionNode<*>) : AlgebraicTypeExpressionNode(left, right, TypeOperator.Sum) {
    override fun infer(leftType: AnyType, rightType: AnyType): IType.Union
        = IType.Union(leftType, rightType)
}
