package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.AttributeInvocationNode
import org.orbit.core.nodes.AttributeMetaTypeExpressionNode
import org.orbit.core.nodes.AttributeOperatorExpressionNode
import org.orbit.util.Invocation

object AttributeMetaTypeExpressionInference : ITypeInference<AttributeMetaTypeExpressionNode, ITypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: AttributeMetaTypeExpressionNode, env: ITypeEnvironment): AnyType = when (val path = Path(node.metaType.value)) {
        Path.any -> AttributeMetaTypeExpression(Always)
        Path.never -> AttributeMetaTypeExpression(Never(""))
        else -> throw invocation.make<TypeSystem>("Invalid Meta Type expression `${path}`. Expected `Any` or `Never`", node)
    }
}

object AttributeOperatorExpressionInference : ITypeInference<AttributeOperatorExpressionNode, ITypeEnvironment> {
    override fun infer(node: AttributeOperatorExpressionNode, env: ITypeEnvironment): AnyType {
        val lType = TypeInferenceUtils.infer(node.leftExpression, env)
            .flatten(Always, env)

        val rType = TypeInferenceUtils.infer(node.rightExpression, env)
            .flatten(Always, env)

        return AttributeTypeOperatorExpression(node.op, lType, rType)
    }
}

object AttributeInvocationInference : ITypeInference<AttributeInvocationNode, IMutableTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: AttributeInvocationNode, env: IMutableTypeEnvironment): AnyType {
        val args = TypeInferenceUtils.inferAll(node.arguments, env)
        val attribute = env.getTypeAs<Attribute>(OrbitMangler.unmangle(node.identifier.getTypeName()))
            ?: throw invocation.make<TypeSystem>("Undefined Type Attribute `${node.identifier.getTypeName()}`", node.identifier)

        val subs = attribute.typeVariables.zip(args)
        val nAttribute = subs.fold(attribute) { acc, next ->
            acc.substitute(Substitution(next)) as Attribute
        }

        return AttributeInvocationExpression(nAttribute, args)
    }
}