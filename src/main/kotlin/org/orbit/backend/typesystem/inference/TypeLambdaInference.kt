package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.OrbitMangler
import org.orbit.core.nodes.*
import org.orbit.util.Invocation
import java.lang.Integer.max

private sealed interface IAttributeInvocationInference<A: IAttributeExpressionNode> : ITypeInference<A, ITypeEnvironment>

private object SingleAttributeInvocationInference : IAttributeInvocationInference<AttributeInvocationNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: AttributeInvocationNode, env: ITypeEnvironment): AnyType {
        val concrete = TypeInferenceUtils.inferAll(node.arguments, env)
        val attrName = node.identifier.getTypeName()

        val attr = env.getTypeAs<IType.Attribute>(OrbitMangler.unmangle(attrName))
            ?: throw invocation.make<TypeSystem>("Undefined Type Attribute `$attrName`", node.identifier)

        return attr.typeVariables.zip(concrete).fold(attr) { acc, next ->
            acc.substitute(Substitution(next.first, next.second)) as IType.Attribute
        }
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
        is AttributeMetaTypeExpressionNode -> TODO()
    }
}

object TypeLambdaInference : ITypeInference<TypeLambdaNode, IMutableTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    private fun constructTypeVariable(node: ITypeLambdaParameterNode) : IType.TypeVar = when (node) {
        is TypeIdentifierNode -> IType.TypeVar(node.getTypeName())
        // TODO - Variadic capture expressions, e.g. `variadic(2+) T`, `variadic(1...9) T`
        is VariadicTypeIdentifierNode -> IType.TypeVar(node.getTypeName(), emptyList(), VariadicBound.Any)
    }

    override fun infer(node: TypeLambdaNode, env: IMutableTypeEnvironment): AnyType {
        val nEnv = env.fork()

        var variadicCount = 0
        var lastVariadicIdx = 0
        val domain = node.domain.mapIndexed { idx, it ->
            val type = constructTypeVariable(it)

            if (type.isVariadic) {
                variadicCount += 1
                lastVariadicIdx = idx
            }

            nEnv.add(type)

            type
        }

        if (variadicCount > 0) {
            if (lastVariadicIdx != domain.count() - 1) {
                throw invocation.make<TypeSystem>("Only the last Type Parameter of a Type Lambda may be Variadic", node)
            }

            if (variadicCount != 1) {
                throw invocation.make<TypeSystem>("Only one Variadic Type Parameter is allowed in a Type Lambda", node)
            }
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