package org.orbit.frontend

import org.orbit.core.SourceProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileReader

class FileSourceProvider(private val path: String) : SourceProvider {
	override fun getSource() : String = File(path).readText()
}