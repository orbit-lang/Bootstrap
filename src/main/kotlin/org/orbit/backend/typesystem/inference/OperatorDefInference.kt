package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.MethodReferenceNode
import org.orbit.core.nodes.OperatorDefNode
import org.orbit.core.nodes.OperatorFixity
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

object OperatorDefInference : ITypeInference<OperatorDefNode> {
    override fun infer(node: OperatorDefNode, env: Env): IType<*> {
        val delegateType = TypeSystemUtils.inferAs<MethodReferenceNode, IType.Signature>(node.methodReferenceNode, env)
        val operator = when (node.fixity) {
            OperatorFixity.Prefix -> IType.PrefixOperator(node.symbol, node.identifierNode.identifier, delegateType.toArrow() as IType.Arrow1)
            OperatorFixity.Infix -> IType.InfixOperator(node.symbol, node.identifierNode.identifier, delegateType.toArrow() as IType.Arrow2)
            OperatorFixity.Postfix -> IType.PostfixOperator(node.symbol, node.identifierNode.identifier, delegateType.toArrow() as IType.Arrow1)
        }

        env.extendInPlace(Decl.Operator(operator))

        return operator
    }
}