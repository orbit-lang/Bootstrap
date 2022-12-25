package org.orbit.frontend

import org.orbit.core.SourceProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileReader

class FileSourceProvider(private val file: File) : SourceProvider {
	override fun getSource() : String = file.readText()
}

class MultiFileSourceProvider(private val files: List<File>) : SourceProvider {
	override fun getSource(): String {
		val innerReaders = files.map(::FileSourceProvider)

		return innerReaders.joinToString("\n") { it.getSource() }
	}
}