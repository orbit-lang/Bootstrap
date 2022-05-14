package org.orbit.types.next.components

import org.orbit.types.next.inference.InferenceUtil

data class ConformanceRefinement(val type: TypeComponent, val trait: ITrait) : ContextualRefinement {
    override fun refine(inferenceUtil: InferenceUtil)
        = inferenceUtil.addConformance(type, trait)
}