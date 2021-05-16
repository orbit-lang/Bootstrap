package org.orbit.frontend

import org.orbit.core.SourceProvider

class StringSourceProvider(private val source: String) : SourceProvider {
	override fun getSource() : String {
		return source
	}
}