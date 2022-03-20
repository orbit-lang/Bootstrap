package org.orbit.types.next.components

import org.orbit.types.next.inference.InferenceResult
import org.orbit.util.Printer

sealed interface MonomorphisationResult<T: TypeComponent> {
    data class Failure<T: TypeComponent>(val input: T) : MonomorphisationResult<T>
    data class Total<T: TypeComponent, R: TypeComponent>(val result: R) : MonomorphisationResult<T>
    data class Partial<T: TypeComponent>(val result: PolymorphicType<T>) : MonomorphisationResult<T>

    fun toType(printer: Printer) : TypeComponent = when (this) {
        is Total<*, *> -> result
        is Partial<*> -> result
        is Failure<*> -> Never("Cannot specialise ${input.toString(printer)}")
    }

    fun toInferenceResult(printer: Printer) : InferenceResult = when (this) {
        is Total<*, *> -> InferenceResult.Success(result)
        is Partial<*> -> InferenceResult.Success(result)
        is Failure<*> -> InferenceResult.Failure(Never("Cannot specialise ${input.toString(printer)}"))
    }
}