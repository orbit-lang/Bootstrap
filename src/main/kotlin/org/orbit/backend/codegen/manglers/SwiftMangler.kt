package org.orbit.backend.codegen.manglers

import org.orbit.core.INameMangler
import org.orbit.core.Path

object SwiftMangler : INameMangler {
    override fun mangle(path: Path): String
        = path.joinToString("_")

    override fun unmangle(name: String): Path
        = Path(name.split("_"))
}