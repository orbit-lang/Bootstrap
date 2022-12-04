package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IMutableTypeEnvironment
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.AttributeInvocationNode
import org.orbit.core.nodes.AttributeOperatorExpressionNode
import org.orbit.util.Invocation

object AttributeOperatorExpressionInference : ITypeInference<AttributeOperatorExpressionNode, ITypeEnvironment> {
    override fun infer(node: AttributeOperatorExpressionNode, env: ITypeEnvironment): AnyType {
        val lType = TypeInferenceUtils.infer(node.leftExpression, env)
        val rType = TypeInferenceUtils.infer(node.rightExpression, env)

        val attr = IType.Attribute("", emptyList()) {
            node.op.apply(lType, rType, env)
        }

        return IType.Attribute.Application(attr, emptyList())
    }
}

object AttributeInvocationInference : ITypeInference<AttributeInvocationNode, IMutableTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: AttributeInvocationNode, env: IMutableTypeEnvironment): AnyType {
        val args = TypeInferenceUtils.inferAll(node.arguments, env)
        val attribute = env.getAllTypes().firstOrNull { it.component is IType.Attribute && it.component.name == node.identifier.getTypeName() }
            ?.component as? IType.Attribute
            ?: throw invocation.make<TypeSystem>("Undefined Type Attribute `${node.identifier.getTypeName()}`", node.identifier)

        return IType.Attribute.Application(attribute, args)
    }
}