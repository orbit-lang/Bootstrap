package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.ProjectedPropertyAssignmentNode

object ProjectedPropertyAssignmentInference : ITypeInference<ProjectedPropertyAssignmentNode, ITypeEnvironment> {
    override fun infer(node: ProjectedPropertyAssignmentNode, env: ITypeEnvironment): AnyType {
        val type = TypeInferenceUtils.infer(node.expression, env)

        return IType.Property(node.identifier.identifier, type)
    }
}