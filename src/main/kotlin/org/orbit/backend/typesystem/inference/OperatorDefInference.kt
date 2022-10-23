package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IMutableTypeEnvironment
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.MethodReferenceNode
import org.orbit.core.nodes.OperatorDefNode
import org.orbit.core.nodes.OperatorFixity

object OperatorDefInference : ITypeInference<OperatorDefNode, IMutableTypeEnvironment> {
    override fun infer(node: OperatorDefNode, env: IMutableTypeEnvironment): AnyType {
        val delegate = TypeInferenceUtils.inferAs<MethodReferenceNode, IType.Signature>(node.methodReferenceNode, env)
        val operator = when (node.fixity) {
            OperatorFixity.Prefix -> IType.PrefixOperator(node.symbol, node.identifierNode.identifier, delegate.toArrow() as IType.Arrow1)
            OperatorFixity.Infix -> IType.InfixOperator(node.symbol, node.identifierNode.identifier, delegate.toArrow() as IType.Arrow2)
            OperatorFixity.Postfix -> IType.PostfixOperator(node.symbol, node.identifierNode.identifier, delegate.toArrow() as IType.Arrow1)
        }

        env.add(operator)

        return operator
    }
}