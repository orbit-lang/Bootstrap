package org.orbit.core

import org.json.JSONObject
import org.orbit.core.nodes.Node
import org.orbit.graph.Annotations
import org.orbit.graph.getAnnotation
import org.orbit.serial.Serial

class Path(vararg val relativeNames: String) : Serial {
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

	override fun describe(json: JSONObject) {
		json.put("path.value", toString(OrbitMangler))
	}
}

fun Node.getPathOrNull() : Path? {
	return getAnnotation<Path>(Annotations.Path)?.value
}

fun Node.getPath() : Path {
	return getAnnotation<Path>(Annotations.Path)!!.value
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