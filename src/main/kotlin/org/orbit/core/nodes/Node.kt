package org.orbit.core.nodes

interface NodeAnnotationTag<T>

data class KeyedNodeAnnotationTag<T>(
	private val identifier: String) : NodeAnnotationTag<T> {

	override fun equals(other: Any?) : Boolean = when (other) {
		null -> false
		else -> {
			other is KeyedNodeAnnotationTag<*>
				&& other.identifier == identifier
		}
	}		
}

data class NodeAnnotation<T>(
	val tag: NodeAnnotationTag<T>?,
	val value: T) {

	override fun equals(other: Any?) : Boolean = when (other) {
		null -> false
		is NodeAnnotation<*> -> other.tag == tag
		else -> false
	}
}

class Node {
	var annotations: Array<NodeAnnotation<*>>

	init {
		annotations = emptyArray()
	}

	inline fun <reified T> annotate(value: T, tag: NodeAnnotationTag<T>) {
		val annotation = NodeAnnotation(tag, value)
		if (annotations.contains(annotation)) {
			throw Exception("Node is already annotated with tag: $tag")
		}
		
		annotations += annotation
	}

	inline fun <reified T> getAnnotation(tag: NodeAnnotationTag<T>) : NodeAnnotation<T>? {
		val results = annotations
			.filterIsInstance<NodeAnnotation<T>>()
			.filter { it.tag == tag }

		return when (results.size) {
			0 -> throw Exception("No annotations found for tag: $tag")
			1 -> results[0]
			else -> throw Exception("Multiple annotations found for tag: $tag")
		}
	}
}