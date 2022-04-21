package org.orbit.core

import java.util.*

data class ScopeIdentifier(private val uuid: String) : AnySerializable() {
	companion object {
		fun next() : ScopeIdentifier {
			return ScopeIdentifier(UUID.randomUUID().toString())
		}
	}
}