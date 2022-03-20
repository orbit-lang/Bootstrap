package org.orbit.types.next.phase

import org.orbit.core.nodes.Node
import org.orbit.types.next.inference.InferenceUtil

data class TypePhaseData<N: Node>(val inferenceUtil: InferenceUtil, val node: N)