package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.AttributeDefNode
import org.orbit.core.nodes.IAttributeExpressionNode
import org.orbit.core.nodes.TypeEffectInvocationNode

object AttributeDefInference : ITypeInference<AttributeDefNode, IMutableTypeEnvironment>, KoinComponent {
    override fun infer(node: AttributeDefNode, env: IMutableTypeEnvironment): AnyType {
        val typeVariables = node.arrow.parameters.map { TypeVar(it.getTypeName()) }
        val nEnv = env.fork()

        typeVariables.forEach { nEnv.add(it) }

        val body = TypeInferenceUtils.inferAs<IAttributeExpressionNode, IAttributeExpression>(node.arrow.constraint, nEnv)
        val effects = TypeInferenceUtils.inferAllAs<TypeEffectInvocationNode, TypeEffect>(node.arrow.effects, nEnv)
        val attribute = Attribute(node.getPath().toString(OrbitMangler), body, typeVariables, effects)

        env.add(attribute)

        return attribute
    }
}