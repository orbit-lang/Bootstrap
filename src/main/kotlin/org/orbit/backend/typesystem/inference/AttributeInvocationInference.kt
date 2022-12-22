package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.components.kinds.KindUtil
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.OrbitMangler
import org.orbit.core.nodes.AttributeInvocationNode
import org.orbit.core.nodes.AttributeOperatorExpressionNode
import org.orbit.core.nodes.ITypeBoundsOperator
import org.orbit.util.Invocation

object AttributeOperatorExpressionInference : ITypeInference<AttributeOperatorExpressionNode, ITypeEnvironment> {
    override fun infer(node: AttributeOperatorExpressionNode, env: ITypeEnvironment): AnyType {
        val lType = TypeInferenceUtils.infer(node.leftExpression, env)
        val rType = TypeInferenceUtils.infer(node.rightExpression, env)

        val proof = when (node.op) {
            is ITypeBoundsOperator.Eq -> IProof.IntrinsicProofs.HasType(lType, rType)
            is ITypeBoundsOperator.KindEq -> IProof.IntrinsicProofs.HasKind(lType, rType)
            is ITypeBoundsOperator.Like -> IProof.IntrinsicProofs.HasTrait(lType, rType)
            is ITypeBoundsOperator.UserDefined -> TODO("UserDefined Proof")
        }

        val attr = IType.Attribute("", emptyList(), listOf(proof)) {
            node.op.apply(lType, rType, env)
        }

        return IType.Attribute.Application(attr, emptyList())
    }
}

object AttributeInvocationInference : ITypeInference<AttributeInvocationNode, IMutableTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: AttributeInvocationNode, env: IMutableTypeEnvironment): AnyType {
        val args = TypeInferenceUtils.inferAll(node.arguments, env)
        val attribute = env.getTypeAs<IType.Attribute>(OrbitMangler.unmangle(node.identifier.getTypeName()))
            ?: throw invocation.make<TypeSystem>("Undefined Type Attribute `${node.identifier.getTypeName()}`", node.identifier)

        val subs = attribute.typeVariables.zip(args)
        val nAttribute = subs.fold(attribute) { acc, next ->
            acc.substitute(Substitution(next)) as IType.Attribute
        }

        return IType.Attribute.Application(nAttribute, args)
    }
}