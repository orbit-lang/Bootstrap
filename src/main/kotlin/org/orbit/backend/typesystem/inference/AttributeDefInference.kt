package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IMutableTypeEnvironment
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.AttributeDefNode

object AttributeDefInference : ITypeInference<AttributeDefNode, IMutableTypeEnvironment> {
    override fun infer(node: AttributeDefNode, env: IMutableTypeEnvironment): AnyType {
        val constraint: (ITypeEnvironment) -> IType.IMetaType<*> = { nEnv ->
            TypeInferenceUtils.inferAs(node.arrow.constraint, nEnv)
        }

        val typeVariables = node.arrow.parameters.map { IType.TypeVar(it.getTypeName()) }
        val attribute = IType.Attribute(node.identifier.getTypeName(), typeVariables, constraint)

        env.add(attribute)

        return attribute
    }
}