package org.orbit.backend.typesystem.components

sealed interface ICaseIterable<Self: ICaseIterable<Self>> : AnyType {
    fun getCases(result: AnyType) : List<Case>
}