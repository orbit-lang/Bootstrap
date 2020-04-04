package org.orbit.core.nodes

import org.json.*
import org.orbit.core.Token

interface NodeAnnotationTag<T> {
	fun toJson() : JSONObject
}

data class KeyedNodeAnnotationTag<T>(
	val key: String) : NodeAnnotationTag<T> {

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

abstract class Node(open val firstToken: Token, open val lastToken: Token) {
	var annotations: Array<NodeAnnotation<*>> = emptyArray()

	final inline fun <reified T> annotate(value: T, tag: NodeAnnotationTag<T>) {
		val annotation = NodeAnnotation(tag, value)
		if (annotations.contains(annotation)) {
			throw Exception("Node is already annotated with tag: $tag")
		}
		
		annotations += annotation
	}

	final inline fun <reified T> annotateByKey(value: T, key: String) {
		annotate(value, KeyedNodeAnnotationTag<T>(key))
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

	inline fun <reified T> getAnnotationByKey(key: String) : NodeAnnotation<T>? {
		return getAnnotation(KeyedNodeAnnotationTag<T>(key))
	}

	final fun getNumberOfAnnotations() : Int {
		return annotations.size
	}

	abstract fun getChildren() : List<Node>

	final fun <N: Node> search(nodeType: Class<N>) : List<N> {
		val matches = getChildren().filterIsInstance(nodeType)

		return matches + getChildren().flatMap { it.search(nodeType) }
	}
}
