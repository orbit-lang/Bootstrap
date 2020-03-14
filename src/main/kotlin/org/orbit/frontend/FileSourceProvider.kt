package org.orbit.frontend

import org.orbit.core.SourceProvider
import java.io.File

class FileSourceProvider(private val path: String) : SourceProvider {
	override fun getSource() : String {
		return File(path).readText()
	}
}