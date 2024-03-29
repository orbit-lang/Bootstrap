package org.orbit.core

import org.orbit.core.nodes.*

class FullyQualifiedPath(override val relativeNames: List<String>) : Path(relativeNames) {
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

// TODO - Move these extensions to somewhere that makes sense
fun INode.getPathOrNull() : Path? {
	return getAnnotation(Annotations.path)?.value
}

fun INode.getPath() : Path {
	return getAnnotation(Annotations.path)!!.value
}

interface INameMangler {
	fun mangle(path: Path) : String
	fun unmangle(name: String) : Path
}

object OrbitMangler : INameMangler {
	override fun mangle(path: Path) : String {
		return path.relativeNames.joinToString("::")
	}

	override fun unmangle(name: String) : Path {
		return Path(name.split("::"))
	}
}

operator fun INameMangler.plus(other: INameMangler) : (String) -> String {
	return {
		other.mangle(unmangle(it))
	}
}