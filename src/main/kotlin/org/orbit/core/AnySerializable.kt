package org.orbit.core

import org.orbit.types.next.components.Signature

sealed class AnySerializable

data class SerialBool(val flag: Boolean) : AnySerializable()
data class SerialIndex(val index: Int) : AnySerializable()
data class SerialSignature(val signature: Signature) : AnySerializable()