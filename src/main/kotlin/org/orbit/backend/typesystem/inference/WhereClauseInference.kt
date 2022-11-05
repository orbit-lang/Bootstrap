package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.WhereClauseNode

object WhereClauseInference : ITypeInference<WhereClauseNode, ITypeEnvironment> {
    override fun infer(node: WhereClauseNode, env: ITypeEnvironment): AnyType
        = TypeInferenceUtils.infer(node.whereExpression, env)
}