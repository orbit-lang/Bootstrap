package org.orbit.core

import org.json.JSONObject
import org.orbit.core.nodes.Node
import org.orbit.graph.Annotations
import org.orbit.graph.Scope
import org.orbit.graph.getAnnotation
import org.orbit.serial.Serial
import java.lang.RuntimeException

open class Path(val relativeNames: List<String>) : Serial {
	constructor(path: String) : this(listOf(path))
	constructor(vararg paths: String) : this(paths.toList())

	var enclosingScope: Scope? = null

	open operator fun plus(other: Path) : Path {
		val a = relativeNames.toList()
		val b = other.relativeNames.toList()
		// NOTE - Really?!
		return Path(a + b)
	}

	fun containsSubPath(other: Path, mangler: Mangler = OrbitMangler) : Boolean {
		return toString(mangler).startsWith(other.toString(mangler))
	}

	fun containsPart(part: String) : Boolean {
		return relativeNames.contains(part)
	}

	fun matchPartial(other: Path) : Boolean {
		return toString(OrbitMangler).startsWith(other.toString(OrbitMangler))
	}

	fun promote() : FullyQualifiedPath = FullyQualifiedPath(this)

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

class FullyQualifiedPath(relativeNames: List<String>) : Path(relativeNames) {
	constructor(path: Path) : this(path.relativeNames)

	fun from(other: String) : FullyQualifiedPath {
		val idx = relativeNames.indexOf(other) + 1

		if (idx < 0) throw RuntimeException("FATAL - TODO")

		val sublist = relativeNames.subList(idx, relativeNames.size)

		return FullyQualifiedPath(sublist)
	}

	override operator fun plus(other: Path) : FullyQualifiedPath {
		return FullyQualifiedPath(relativeNames + other.relativeNames)
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
		return Path(name.split("::"))
	}
}