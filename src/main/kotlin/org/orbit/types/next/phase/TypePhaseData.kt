package org.orbit.types.next.phase

import org.orbit.core.nodes.INode
import org.orbit.types.next.inference.InferenceUtil

data class TypePhaseData<N: INode>(val inferenceUtil: InferenceUtil, val node: N)