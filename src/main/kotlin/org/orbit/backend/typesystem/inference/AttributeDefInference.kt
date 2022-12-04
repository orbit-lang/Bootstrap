package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IMutableTypeEnvironment
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.AttributeDefNode
import org.orbit.core.nodes.IAttributeExpressionNode
import org.orbit.util.Invocation

object AttributeDefInference : ITypeInference<AttributeDefNode, IMutableTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: AttributeDefNode, env: IMutableTypeEnvironment): AnyType {
        val constraint: (IMutableTypeEnvironment) -> IType.IMetaType<*> = { nEnv ->
            val app = TypeInferenceUtils.inferAs<IAttributeExpressionNode, IType.Attribute.IAttributeApplication>(node.arrow.constraint, nEnv)

            when (val result = app.invoke(nEnv)) {
                is IType.Never -> result.panic()
                else -> result
            }
        }

        val typeVariables = node.arrow.parameters.map { IType.TypeVar(it.getTypeName()) }
        val attribute = IType.Attribute(node.identifier.getTypeName(), typeVariables, constraint)

        env.add(attribute)

        return attribute
    }
}