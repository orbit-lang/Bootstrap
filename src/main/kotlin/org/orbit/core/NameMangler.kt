package org.orbit.core

import org.orbit.core.nodes.Node
import org.orbit.core.nodes.NodeAnnotationTag
import org.orbit.core.nodes.Annotations
import org.orbit.types.next.components.Signature
import org.orbit.types.next.components.TypeComponent

class FullyQualifiedPath(override val relativeNames: List<String>) : Path(relativeNames) {
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
	return getAnnotation(Annotations.Path as NodeAnnotationTag<Path>)?.value
}

fun Node.getPath() : Path {
	return getAnnotation(Annotations.Path as NodeAnnotationTag<Path>)!!.value
}

interface Mangler {
	fun mangle(path: Path) : String
	fun unmangle(name: String) : Path
	fun mangle(signature: Signature) : String
}

object OrbitMangler : Mangler {
	override fun mangle(path: Path) : String {
		return path.relativeNames.joinToString("::")
	}

	override fun unmangle(name: String) : Path {
		return Path(name.split("::"))
	}

	override fun mangle(signature: Signature): String {
		val mang = (OrbitMangler + OrbitMangler)
		val receiver = mang(signature.receiver.fullyQualifiedName)
		val params = signature.parameters.map(TypeComponent::fullyQualifiedName)
			.joinToString(", ", transform = mang)

		val ret = mang(signature.returns.fullyQualifiedName)

		return "($receiver) ${signature.relativeName} ($params) ($ret)"
	}
}

operator fun Mangler.plus(other: Mangler) : (String) -> String {
	return {
		other.mangle(unmangle(it))
	}
}