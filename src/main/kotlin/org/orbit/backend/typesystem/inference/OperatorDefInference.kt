package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Decl
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.utils.TypeSystemUtilsOLD
import org.orbit.core.nodes.MethodReferenceNode
import org.orbit.core.nodes.OperatorDefNode
import org.orbit.core.nodes.OperatorFixity

object OperatorDefInference : ITypeInferenceOLD<OperatorDefNode> {
    override fun infer(node: OperatorDefNode, env: Env): AnyType {
        val delegateType = TypeSystemUtilsOLD.inferAs<MethodReferenceNode, IType.Signature>(node.methodReferenceNode, env)
        val operator = when (node.fixity) {
            OperatorFixity.Prefix -> IType.PrefixOperator(node.symbol, node.identifierNode.identifier, delegateType.toArrow() as IType.Arrow1)
            OperatorFixity.Infix -> IType.InfixOperator(node.symbol, node.identifierNode.identifier, delegateType.toArrow() as IType.Arrow2)
            OperatorFixity.Postfix -> IType.PostfixOperator(node.symbol, node.identifierNode.identifier, delegateType.toArrow() as IType.Arrow1)
        }

        env.extendInPlace(Decl.Operator(operator))

        return operator
    }
}