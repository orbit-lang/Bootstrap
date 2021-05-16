package org.orbit.core.nodes

import org.json.JSONObject
import org.orbit.core.components.Token
import org.orbit.frontend.rules.PhaseAnnotationNode
import org.orbit.graph.pathresolvers.PathResolver
import org.orbit.serial.Serial
import org.orbit.serial.Serialiser
import org.orbit.util.PriorityComparator
import org.orbit.util.prioritise

interface NodeAnnotationTag<T: Serial> : Serial {
	override fun describe(json: JSONObject) {
		json.put("annotation.tag.type", javaClass.simpleName)
	}
}

data class KeyedNodeAnnotationTag<T: Serial>(val key: String) : NodeAnnotationTag<T> {
	override fun equals(other: Any?) : Boolean = when (other) {
		null -> false
		else -> {
			other is KeyedNodeAnnotationTag<*>
				&& other.key == key
		}
	}

	override fun describe(json: JSONObject) {
		super.describe(json)

		json.put("annotation.tag.key", key)
	}
}

data class NodeAnnotation<T: Serial>(
	val tag: NodeAnnotationTag<T>?,
	val value: T) : Serial {

	override fun equals(other: Any?) : Boolean = when (other) {
		null -> false
		is NodeAnnotation<*> -> other.tag == tag
		else -> false
	}

	override fun describe(json: JSONObject) {
		json.put("annotation.tag", Serialiser.serialise(tag))
		json.put("annotation.value", Serialiser.serialise(value))
	}
}

abstract class Node(open val firstToken: Token, open val lastToken: Token) : Serial {
	interface MapFilter<N> {
		fun filter(node: Node) : Boolean
		fun map(node: Node) : List<N>
	}

	var annotations: MutableSet<NodeAnnotation<*>> = mutableSetOf()
	var phaseAnnotationNodes = mutableListOf<PhaseAnnotationNode>()

	val range: IntRange
		get() = IntRange(firstToken.position.absolute, lastToken.position.absolute)

	fun insertPhaseAnnotation(phaseAnnotationNode: PhaseAnnotationNode) {
		phaseAnnotationNodes.add(phaseAnnotationNode)
	}

	inline fun <reified T: Serial> annotate(value: T, tag: NodeAnnotationTag<T>, mergeOnConflict: Boolean = false) {
		val annotation = NodeAnnotation(tag, value)

		if (mergeOnConflict && annotations.any { it.tag == tag }) {
			annotations.removeAll { it.tag == tag }
		}

		annotations.add(annotation)
	}

	inline fun <reified T: Serial> annotateByKey(value: T, key: String, mergeOnConflict: Boolean = false) {
		annotate(value, KeyedNodeAnnotationTag(key), mergeOnConflict)
	}

	inline fun <reified T: Serial> getAnnotation(tag: NodeAnnotationTag<T>) : NodeAnnotation<T>? {
		val results = annotations
			.filterIsInstance<NodeAnnotation<T>>()
			.filter { it.tag == tag }

		return when (results.size) {
			0 -> null
			1 -> results[0]
			else -> throw Exception("Multiple annotations found for tag: $tag")
		}
	}

	inline fun <reified T: Serial> getAnnotationByKey(key: String) : NodeAnnotation<T>? {
		return getAnnotation(KeyedNodeAnnotationTag<T>(key))
	}

	final fun getNumberOfAnnotations() : Int {
		return annotations.size
	}

	abstract fun getChildren() : List<Node>

	inline fun <reified N: Node> search(priorityComparator: PriorityComparator<N>? = null) : List<N> {
		return search(N::class.java)
	}

	fun <N: Node> search(nodeType: Class<N>, priorityComparator: PriorityComparator<N>? = null) : List<N> {
		val matches = getChildren().filterIsInstance(nodeType)

		return matches + getChildren().flatMap { it.search(nodeType) }
			.prioritise(priorityComparator)
	}

	fun <N: Node> search(mapFilter: Node.MapFilter<N>) : List<N> {
		val mine = getChildren()
			.filter(mapFilter::filter)
			.flatMap(mapFilter::map)

		return mine + getChildren().flatMap { it.search(mapFilter) }
	}

	override fun describe(json: JSONObject) {
		json.put("node.type", javaClass.simpleName)

		val ann = annotations.map { Serialiser.serialise(it) }

		json.put("node.annotations", ann)
		json.put("node.phaseAnnotations", phaseAnnotationNodes)

		val children = getChildren().map { Serialiser.serialise(it) }

		json.put("node.children", children)
	}
}

abstract class AnnotatedNode(firstToken: Token, lastToken: Token, open val annotationPass: PathResolver.Pass) : Node(firstToken, lastToken)
