package org.orbit.main

import org.orbit.backend.codegen.utils.ICodeGenTarget
import java.io.File

data class BuildConfig(val maxDepth: Int, val productName: String, val outputPath: File)