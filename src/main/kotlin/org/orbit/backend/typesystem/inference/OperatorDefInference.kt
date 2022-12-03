package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IMutableTypeEnvironment
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.AnyArrow
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.*
import org.orbit.util.Invocation

object OperatorDefInference : ITypeInference<OperatorDefNode, IMutableTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    private inline fun <reified A: AnyArrow> checkArrow(node: OperatorDefNode, fixity: OperatorFixity, delegate: AnyArrow) : A = when (delegate) {
        is IType.Signature -> when (val a = delegate.toArrow()) {
            is A -> a
            else -> throw invocation.make<TypeSystem>(
                "$fixity operator `${node.identifierNode.identifier}` cannot delegate by method reference $delegate because it does not accept the correct number of parameters: expected ${fixity.numberOfParameters}, found ${delegate.parameters.count()}",
                node
            )
        }

        else -> when (delegate) {
            is A -> delegate
            else -> throw invocation.make<TypeSystem>(
                "$fixity operator `${node.identifierNode.identifier}` cannot delegate by method reference $delegate because it does not accept the correct number of parameters: expected ${fixity.numberOfParameters}, found ${delegate.getDomain().count()}",
                node
            )
        }
    }

    override fun infer(node: OperatorDefNode, env: IMutableTypeEnvironment): AnyType {
        val delegate = TypeInferenceUtils.inferAs<IInvokableDelegateNode, AnyArrow>(node.invokableDelegate, env)
        val operator = when (node.fixity) {
            OperatorFixity.Prefix -> IType.PrefixOperator(node.symbol, node.identifierNode.identifier, checkArrow(node, node.fixity, delegate))
            OperatorFixity.Infix -> IType.InfixOperator(node.symbol, node.identifierNode.identifier, checkArrow(node, node.fixity, delegate))
            OperatorFixity.Postfix -> IType.PostfixOperator(node.symbol, node.identifierNode.identifier, checkArrow(node, node.fixity, delegate))
        }

        env.add(operator)

        return operator
    }
}