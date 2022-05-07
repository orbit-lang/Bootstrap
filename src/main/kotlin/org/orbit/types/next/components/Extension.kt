package org.orbit.types.next.components

import org.orbit.types.next.inference.InferenceUtil

interface Extension<T: TypeComponent> {
    val baseType: T

    fun extend(inferenceUtil: InferenceUtil)
}