package org.orbit.core.nodes

import org.json.*

interface NodeAnnotationTag<T> {
	fun toJson() : JSONObject
}

data class KeyedNodeAnnotationTag<T>(
	private val key: String) : NodeAnnotationTag<T> {

	override fun equals(other: Any?) : Boolean = when (other) {
		null -> false
		else -> {
			other is KeyedNodeAnnotationTag<*>
				&& other.key == key
		}
	}

	override fun toJson() : JSONObject {
		val json = JSONObject()

		json.put("tag.simpleName", this::class.java.getSimpleName())
		json.put("tag.canonicalName", this::class.java.getCanonicalName())
		json.put("tag.key", key)

		return json
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

abstract class Node {
	var annotations: Array<NodeAnnotation<*>> = emptyArray()

	final inline fun <reified T> annotate(value: T, tag: NodeAnnotationTag<T>) {
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

	final fun getNumberOfAnnotations() : Int {
		return annotations.size
	}
}