package org.orbit.types.next.components

import org.orbit.types.next.inference.InferenceUtil
import org.orbit.types.next.utils.RefinementErrorWriter

data class EqualityRefinement(val typeVariable: TypeComponent, val concreteType: TypeComponent) : ContextualRefinement {
    override fun refine(inferenceUtil: InferenceUtil) = when (inferenceUtil.find(typeVariable.fullyQualifiedName)) {
        null -> RefinementErrorWriter.missing(typeVariable)
        is TypeVariable -> {
            val a = inferenceUtil.find(typeVariable.fullyQualifiedName)!!
            val b = inferenceUtil.find(concreteType.fullyQualifiedName)!!

            if (a.fullyQualifiedName == b.fullyQualifiedName) {
                RefinementErrorWriter.cyclicConstraint(typeVariable, concreteType)
            }

            inferenceUtil.declare(Alias(typeVariable.fullyQualifiedName, concreteType))
        }
        else -> when (typeVariable.fullyQualifiedName == concreteType.fullyQualifiedName) {
            true -> {}
            else -> RefinementErrorWriter.doubleRefinement(typeVariable)
        }
    }
}