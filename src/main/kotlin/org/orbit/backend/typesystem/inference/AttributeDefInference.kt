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
        val typeVariables = node.arrow.parameters.map { IType.TypeVar(it.getTypeName()) }
        val attribute = IType.Attribute(node.getPath().toString(OrbitMangler), node.arrow.constraint, typeVariables)

        env.add(attribute)

        return attribute
    }
}