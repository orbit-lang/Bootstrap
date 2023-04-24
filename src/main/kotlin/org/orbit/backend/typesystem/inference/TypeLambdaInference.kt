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

        val attr = env.getTypeAs<Attribute>(OrbitMangler.unmangle(attrName))
            ?: throw invocation.make<TypeSystem>("Undefined Type Attribute `$attrName`", node.identifier)

        return attr.typeVariables.zip(concrete).fold(attr) { acc, next ->
            acc.substitute(Substitution(next.first, next.second)) as Attribute
        }
    }
}

private object CompoundAttributeInvocationInference : IAttributeInvocationInference<CompoundAttributeExpressionNode>, KoinComponent {
    override fun infer(node: CompoundAttributeExpressionNode, env: ITypeEnvironment): AnyType {
        val lAttribute = AnyAttributeInvocationInference.infer(node.leftExpression, env) as IAttribute
        val rAttribute = AnyAttributeInvocationInference.infer(node.rightExpression, env) as IAttribute

        return CompoundAttribute(node.op, lAttribute, rAttribute)
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

    private fun constructTypeVariable(node: ITypeLambdaParameterNode, env: ITypeEnvironment) : TypeVar = when (node) {
        is TypeIdentifierNode -> TypeVar(node.getTypeName())
        // TODO - Variadic capture expressions, e.g. `variadic(2+) T`, `variadic(1...9) T`
        is VariadicTypeIdentifierNode -> TypeVar(node.getTypeName(), emptyList(), VariadicBound.Any)
        is DependentTypeParameterNode -> {
            val dependentType = TypeInferenceUtils.infer(node.type, env)

            TypeVar(node.identifier.getTypeName(), emptyList(), null, dependentType)
        }
    }

    override fun infer(node: TypeLambdaNode, env: IMutableTypeEnvironment): AnyType {
        val nEnv = env.fork()

        var variadicCount = 0
        var lastVariadicIdx = 0
        val domain = node.domain.mapIndexed { idx, it ->
            val type = constructTypeVariable(it, env)

            if (type.isVariadic) {
                variadicCount += 1
                lastVariadicIdx = idx
            }

            nEnv.add(type)

            type
        }

        val vIndices = when (variadicCount) {
            0 -> emptyList()
            else -> {
                if (lastVariadicIdx != domain.count() - 1) {
                    throw invocation.make<TypeSystem>("Only the last Type Parameter of a Type Lambda may be Variadic", node)
                }

                if (variadicCount != 1) {
                    throw invocation.make<TypeSystem>("Only one Variadic Type Parameter is allowed in a Type Lambda", node)
                }

                node.codomain.search(IndexSliceNode::class.java)
                    .map { it.index }
            }
        }

        val attributes = node.constraints.map {
            AnyAttributeInvocationInference.infer(it.invocation, nEnv) as IAttribute
        }

        val mEnv = AttributedEnvironment(nEnv, attributes)
        val codomain = TypeInferenceUtils.infer(node.codomain, mEnv)

        if (codomain is TypeVar && codomain.isVariadic) {
            val example = VariadicSlice(codomain, 0)
            throw invocation.make<TypeSystem>("Cannot return Variadic Type Parameter $codomain. Suggest slicing instead, e.g. `$example`", node.codomain)
        }

        val fallback = when (node.elseClause) {
            null -> null
            else -> TypeInferenceUtils.infer(node.elseClause, mEnv)
        }

        return ConstrainedArrow(domain.arrowOf(codomain), attributes, fallback, emptyList(), vIndices)
    }
}