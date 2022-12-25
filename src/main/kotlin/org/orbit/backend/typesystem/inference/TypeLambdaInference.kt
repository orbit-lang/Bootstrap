package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.OrbitMangler
import org.orbit.core.nodes.*
import org.orbit.util.Invocation

private sealed interface IAttributeInvocationInference<A: IAttributeExpressionNode> : ITypeInference<A, ITypeEnvironment>

private object SingleAttributeInvocationInference : IAttributeInvocationInference<AttributeInvocationNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: AttributeInvocationNode, env: ITypeEnvironment): AnyType {
        val concrete = TypeInferenceUtils.inferAll(node.arguments, env)
        val attrName = node.identifier.getTypeName()

        val attr = env.getTypeAs<IType.Attribute>(OrbitMangler.unmangle(attrName))
            ?: throw invocation.make<TypeSystem>("Undefined Type Attribute `$attrName`", node.identifier)

        return attr.abstractTypes.zip(concrete).fold(attr) { acc, next -> acc.substitute(Substitution(next.first, next.second)) as IType.Attribute }
    }
}

private object CompoundAttributeInvocationInference : IAttributeInvocationInference<CompoundAttributeExpressionNode>, KoinComponent {
    override fun infer(node: CompoundAttributeExpressionNode, env: ITypeEnvironment): AnyType {
        val lAttribute = AnyAttributeInvocationInference.infer(node.leftExpression, env) as IType.IAttribute
        val rAttribute = AnyAttributeInvocationInference.infer(node.rightExpression, env) as IType.IAttribute

        return IType.CompoundAttribute(node.op, lAttribute, rAttribute)
    }
}

private object AnyAttributeInvocationInference : IAttributeInvocationInference<IAttributeExpressionNode> {
    override fun infer(node: IAttributeExpressionNode, env: ITypeEnvironment): AnyType = when (node) {
        is AttributeInvocationNode -> SingleAttributeInvocationInference.infer(node, env)
        is AttributeOperatorExpressionNode -> TODO()
        is CompoundAttributeExpressionNode -> CompoundAttributeInvocationInference.infer(node, env)
    }
}

object TypeLambdaInference : ITypeInference<TypeLambdaNode, IMutableTypeEnvironment>, KoinComponent {
    override fun infer(node: TypeLambdaNode, env: IMutableTypeEnvironment): AnyType {
        val nEnv = env.fork()

        val domain = node.domain.map {
            val type = IType.TypeVar(it.getTypeName())

            nEnv.add(type)

            type
        }

        val attributes = node.constraints.map {
            AnyAttributeInvocationInference.infer(it.invocation, nEnv) as IType.IAttribute
        }

        val mEnv = AttributedEnvironment(nEnv, attributes)
        val codomain = TypeInferenceUtils.infer(node.codomain, mEnv)
        val fallback = when (node.elseClause) {
            null -> null
            else -> TypeInferenceUtils.infer(node.elseClause, mEnv)
        }

        return IType.ConstrainedArrow(domain.arrowOf(codomain), attributes, fallback)
    }
}