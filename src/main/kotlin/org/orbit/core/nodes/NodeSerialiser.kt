package org.orbit.core.nodes

import org.json.JSONObject

interface Serialiser<T, O> {
	fun serialise(obj: T) : O
}

interface NodeAnnotationSerialiser<O> : Serialiser<NodeAnnotation<*>, O>
interface NodeSerialiser<N: Node, O> : Serialiser<N, O>

object JsonNodeAnnotationSerialiser : NodeAnnotationSerialiser<JSONObject> {
	override fun serialise(obj: NodeAnnotation<*>) : JSONObject {
		val json = JSONObject()

		json.put("annotation.simpleName", obj::class.java.getSimpleName())
		json.put("annotation.canonicalName", obj::class.java.getCanonicalName())
		json.put("annotation.tag", obj.tag?.toJson())
		// TODO - Find a better way to serialise annotation values
		json.put("annotation.value", obj.value.toString())
		
		return json
	}
}

interface JsonNodeSerialiser<N: Node> : NodeSerialiser<N, JSONObject> {
	fun jsonify(node: Node) : JSONObject {
		val json = JSONObject()

		json.put("node.simpleName", node::class.java.getSimpleName())
		json.put("node.canonicalName", node::class.java.getCanonicalName())
		json.put("node.annotations", node.annotations.map { JsonNodeAnnotationSerialiser.serialise(it) })
		
		return json
	}
}