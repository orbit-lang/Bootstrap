package org.orbit.types.next.components

import org.orbit.types.next.inference.InferenceUtil

interface ContextualRefinement {
    fun refine(inferenceUtil: InferenceUtil)
}