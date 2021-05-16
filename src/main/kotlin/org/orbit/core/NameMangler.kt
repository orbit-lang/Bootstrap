package org.orbit.core

import org.json.JSONObject
import org.orbit.core.nodes.Node
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Scope
import org.orbit.graph.extensions.getAnnotation
import org.orbit.serial.Serial
import org.orbit.types.components.InstanceSignature
import org.orbit.types.components.Parameter
import org.orbit.types.components.TypeProtocol
import org.orbit.types.components.TypeSignature

open class Path(val relativeNames: List<String>) : Serial {
	companion object {
		val empty = Path()
	}

	constructor(path: String) : this(listOf(path))
	constructor(vararg paths: String) : this(paths.toList())

	var enclosingScope: Scope? = null

	open operator fun plus(other: Path) : Path {
		val a = relativeNames.toList()
		val b = other.relativeNames.toList()
		// NOTE - Really?!
		return Path(a + b)
	}

	operator fun plus(other: String) : Path {
		return this + Path(other)
	}

	open operator fun minus(other: Path) : Path {
		return Path(relativeNames.subList(0, relativeNames.indexOf(other.relativeNames.last())))
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
	constructor(vararg relativeNames: String) : this(relativeNames.toList())

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
	fun mangle(signature: InstanceSignature) : String
	fun mangle(signature: TypeSignature) : String
}

object OrbitMangler : Mangler {
	override fun mangle(path: Path) : String {
		return path.relativeNames.joinToString("::")
	}

	override fun unmangle(name: String) : Path {
		return Path(name.split("::"))
	}

	override fun mangle(signature: InstanceSignature): String {
		val mang = (OrbitMangler + OrbitMangler)
		val receiver = mang(signature.receiver.type.name)
		val params = signature.parameters.map(Parameter::type)
			.map(TypeProtocol::name).joinToString(", ", transform = mang)

		val ret = mang(signature.returnType.name)

		return "($receiver) ${signature.name} ($params) ($ret)"
	}

	override fun mangle(signature: TypeSignature): String {
		val mang = (OrbitMangler + OrbitMangler)
		val receiver = mang(signature.receiver.name)
		val params = signature.parameters.map(Parameter::type)
			.map(TypeProtocol::name).joinToString(", ", transform = mang)

		val ret = mang(signature.returnType.name)

		return "($receiver) ${signature.name} ($params) ($ret)"
	}
}

operator fun Mangler.plus(other: Mangler) : (String) -> String {
	return {
		other.mangle(unmangle(it))
	}
}