package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IMutableTypeEnvironment
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.fork
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.AttributeDefNode
import org.orbit.core.nodes.IAttributeExpressionNode
import org.orbit.util.Invocation

object AttributeDefInference : ITypeInference<AttributeDefNode, IMutableTypeEnvironment>, KoinComponent {
    override fun infer(node: AttributeDefNode, env: IMutableTypeEnvironment): AnyType {
        val constraint: (IMutableTypeEnvironment) -> IType.IMetaType<*> = { nEnv ->
            val app = TypeInferenceUtils.inferAs<IAttributeExpressionNode, IType.Attribute.IAttributeApplication>(node.arrow.constraint, nEnv)

            when (val result = app.invoke(nEnv)) {
                is IType.Never -> result.panic()
                else -> result
            }
        }

        val typeVariables = node.arrow.parameters.map { IType.TypeVar(it.getTypeName()) }

        val nEnv = env.fork()

        typeVariables.forEach { nEnv.add(it) }

        val pConstraint = TypeInferenceUtils.inferAs<IAttributeExpressionNode, IType.Attribute.IAttributeApplication>(node.arrow.constraint, nEnv)
        val attribute = IType.Attribute(node.getPath().toString(OrbitMangler), typeVariables, pConstraint.getOriginalAttribute().proofs, constraint)

        env.add(attribute)

        return attribute
    }
}