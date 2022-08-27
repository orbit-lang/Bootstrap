package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.components.TypeOperator
import org.orbit.precess.backend.utils.AnyType

abstract class AlgebraicTypeExpressionNode<T: IType.IAlgebraicType<T>>(open val left: TypeExpressionNode<*>, open val right: TypeExpressionNode<*>, open val op: TypeOperator) : TypeExpressionNode<T>() {
    override fun getChildren(): List<Node> = listOf(left, right)
    override fun toString(): String = "($left ${op.symbol} $right)"

    abstract fun infer(leftType: AnyType, rightType: AnyType) : T

    override fun infer(env: Env): T {
        val leftType = left.infer(env)
        val rightType = right.infer(env)

        return infer(leftType, rightType)
    }
}

data class ProductTypeExpressionNode(override val firstToken: Token, override val lastToken: Token, override val left: TypeExpressionNode<*>, override val right: TypeExpressionNode<*>) : AlgebraicTypeExpressionNode<IType.Tuple>(left, right, TypeOperator.Product) {
    override fun infer(leftType: AnyType, rightType: AnyType): IType.Tuple
        = IType.Tuple(leftType, rightType)
}

data class SumTypeExpressionNode(override val firstToken: Token, override val lastToken: Token, override val left: TypeExpressionNode<*>, override val right: TypeExpressionNode<*>) : AlgebraicTypeExpressionNode<IType.Union>(left, right, TypeOperator.Sum) {
    override fun infer(leftType: AnyType, rightType: AnyType): IType.Union
        = IType.Union(leftType, rightType)
}
