package org.orbit.types.next.components

import org.orbit.types.next.inference.InferenceUtil

data class RefinementExtension<T: TypeComponent>(override val baseType: PolymorphicType<T>) : Extension<PolymorphicType<T>> {
    override fun extend(inferenceUtil: InferenceUtil) {

    }
}
