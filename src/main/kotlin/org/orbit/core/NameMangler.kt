package org.orbit.core

import org.orbit.core.nodes.Node
import org.orbit.graph.components.Annotations
import org.orbit.graph.extensions.getAnnotation
import org.orbit.types.components.Parameter
import org.orbit.types.components.Type
import org.orbit.types.components.TypeProtocol
import org.orbit.types.components.TypeSignature

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

fun Node.getType() : TypeProtocol {
	val type = getAnnotation<TypeProtocol>(Annotations.Type)
		?: throw RuntimeException("HERE")

	return type!!.value
}

fun Type.getFullyQualifiedPath() : Path {
	return properties.fold(OrbitMangler.unmangle(name)) { acc, next ->
		acc + OrbitMangler.unmangle(next.type.name)
	}
}

interface Mangler {
	fun mangle(path: Path) : String
	fun unmangle(name: String) : Path
	fun mangle(signature: TypeSignature) : String
}

object OrbitMangler : Mangler {
	override fun mangle(path: Path) : String {
		return path.relativeNames.joinToString("::")
	}

	override fun unmangle(name: String) : Path {
		return Path(name.split("::"))
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