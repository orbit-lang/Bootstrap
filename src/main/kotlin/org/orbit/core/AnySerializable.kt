package org.orbit.core

sealed class AnySerializable

data class SerialBool(val flag: Boolean) : AnySerializable()
data class SerialIndex(val index: Int) : AnySerializable()