package org.orbit.core.nodes

import org.orbit.core.AnySerializable
import org.orbit.core.components.Token
import org.orbit.graph.pathresolvers.PathResolver
import org.orbit.util.PriorityComparator
import org.orbit.util.prioritise

sealed class NodeAnnotationTag<T: AnySerializable>

data class KeyedNodeAnnotationTag<T: AnySerializable>(val key: String) : NodeAnnotationTag<T>() {
	override fun equals(other: Any?) : Boolean = when (other) {
		null -> false
		else -> {
			other is KeyedNodeAnnotationTag<*>
				&& other.key == key
		}
	}
}

data class NodeAnnotation<T: AnySerializable>(val tag: NodeAnnotationTag<T>?, val value: T) {
	override fun equals(other: Any?) : Boolean = when (other) {
		null -> false
		is NodeAnnotation<*> -> other.tag == tag
		else -> false
	}
}

interface ScopedNode

abstract class Node {
	abstract val firstToken: Token
	abstract val lastToken: Token

	interface MapFilter<N> {
		fun filter(node: Node) : Boolean
		fun map(node: Node) : List<N>
	}

	val id: String = "${javaClass.simpleName}@${java.util.UUID.randomUUID()}"

	var annotations: MutableSet<NodeAnnotation<*>> = mutableSetOf()
	var phaseAnnotationNodes = mutableListOf<PhaseAnnotationNode>()

	val range: IntRange
		get() = IntRange(firstToken.position.absolute, lastToken.position.absolute)

	fun insertPhaseAnnotation(phaseAnnotationNode: PhaseAnnotationNode) {
		phaseAnnotationNodes.add(phaseAnnotationNode)
	}

	inline fun <reified T: AnySerializable> annotate(value: T, tag: NodeAnnotationTag<T>, mergeOnConflict: Boolean = false) {
		val annotation = NodeAnnotation(tag, value)

		if (mergeOnConflict && annotations.any { it.tag == tag }) {
			annotations.removeAll { it.tag == tag }
		}

		annotations.add(annotation)
	}

	inline fun <reified T: AnySerializable> annotateByKey(value: T, key: String, mergeOnConflict: Boolean = false) {
		annotate(value, KeyedNodeAnnotationTag(key), mergeOnConflict)
	}

	inline fun <reified T: AnySerializable> getAnnotation(tag: NodeAnnotationTag<T>) : NodeAnnotation<T>? {
		val results = annotations
			.filterIsInstance<NodeAnnotation<T>>()
			.filter { it.tag == tag }

		return when (results.size) {
			0 -> null
			1 -> results[0]
			else -> {
				if (results.all { it == results[0] }) {
					// If all the same, we've just accidentally annotated more than once with the same value
					return results[0]
				}

				throw Exception("Multiple annotations found for tag: $tag")
			}
		}
	}

	inline fun <reified T: AnySerializable> getAnnotationByKey(key: String) : NodeAnnotation<T>? {
		return getAnnotation(KeyedNodeAnnotationTag(key))
	}

	fun getNumberOfAnnotations() : Int {
		return annotations.size
	}

	abstract fun getChildren() : List<Node>

	inline fun <reified N: Node> search(priorityComparator: PriorityComparator<N>? = null, ignoreScopedNodes: Boolean = false) : List<N> {
		return search(N::class.java, ignoreScopedNodes = ignoreScopedNodes)
	}

	fun <N: Node> search(nodeType: Class<N>, priorityComparator: PriorityComparator<N>? = null, ignoreScopedNodes: Boolean = false) : List<N> {
		val matches = getChildren().filterIsInstance(nodeType)
			.filter {
				when (ignoreScopedNodes && it is ScopedNode) {
					true -> false
					else -> true
				}
			}

		return matches + getChildren().flatMap { it.search(nodeType, ignoreScopedNodes = ignoreScopedNodes) }
			.prioritise(priorityComparator)
	}

	fun <N: Node> search(mapFilter: Node.MapFilter<N>) : List<N> {
		val mine = getChildren()
			.filter(mapFilter::filter)
			.flatMap(mapFilter::map)

		return mine + getChildren().flatMap { it.search(mapFilter) }
	}
}

abstract class AnnotatedNode : Node() {
	abstract val annotationPass: PathResolver.Pass
}

abstract class BoundNode : Node()

fun Node.prettyPrintEmpty(depth: Int = 0) : String
	= "${" ".repeat(depth)}${javaClass.simpleName}"

fun Node.prettyPrintNonEmpty(depth: Int = 0) : String
	= "${" ".repeat(depth)}${javaClass.simpleName}\n${getChildren().joinToString("\n") { it.prettyPrint(depth + 1) }}"

fun Node.prettyPrint(depth: Int = 0) : String = when (getChildren().isEmpty()) {
	true -> prettyPrintEmpty(depth)
	else -> prettyPrintNonEmpty(depth)
}
