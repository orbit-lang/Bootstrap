package org.orbit.core

sealed class AnySerializable

data class SerialBool(val flag: Boolean) : AnySerializable()