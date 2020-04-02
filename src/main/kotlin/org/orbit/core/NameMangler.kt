package org.orbit.core

final class Path(vararg val relativeNames: String) {
	operator fun plus(other: Path) : Path {
		val a = relativeNames.toList()
		val b = other.relativeNames.toList()
		// NOTE - Really?!
		return Path(*(a + b).toTypedArray())
	}

	fun toString(mangler: Mangler) : String {
		return mangler.mangle(this)
	}

	override fun equals(other: Any?) = when (other) {
		is Path -> other.relativeNames.joinToString("") == relativeNames.joinToString("")
		else -> false
	}
}

interface Mangler {
	fun mangle(path: Path) : String
	fun unmangle(name: String) : Path
}

object OrbitMangler : Mangler {
	override fun mangle(path: Path) : String {
		return path.relativeNames.joinToString("::")
	}

	override fun unmangle(name: String) : Path {
		return Path(*name.split("::").toTypedArray())
	}
}