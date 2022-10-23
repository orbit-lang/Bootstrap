package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.PairNode

// NOTE - A `Pair` does not refer to the Kotlin concept of a Pair, but rather a name-type pair, e.g. (i Int)
object PairInference : ITypeInference<PairNode, ITypeEnvironment> {
    override fun infer(node: PairNode, env: ITypeEnvironment): AnyType
        = TypeInferenceUtils.infer(node.typeExpressionNode, env)
}