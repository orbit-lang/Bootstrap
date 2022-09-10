package org.orbit.core.nodes

import org.orbit.core.components.Token
import org.orbit.util.PriorityComparator
import org.orbit.util.getKoinInstance
import org.orbit.util.prioritise

interface INodeAnnotationTag<T> {
	val key: String
}

data class NodeAnnotationTag<T>(override val key: String) : INodeAnnotationTag<T> {
	override fun equals(other: Any?) : Boolean = when (other) {
		is NodeAnnotationTag<*> -> other.key == key
		else -> false
	}

	override fun hashCode(): Int {
		return key.hashCode()
	}
}

data class NodeAnnotation<T>(val tag: INodeAnnotationTag<T>?, val value: T) {
	override fun equals(other: Any?) : Boolean = when (other) {
		is NodeAnnotation<*> -> other.tag == tag
		else -> false
	}

	override fun hashCode(): Int {
		var result = tag?.hashCode() ?: 0
		result = 31 * result + value.hashCode()
		return result
	}
}

class NodeAnnotationMap {
	private val nodeAnnotationMap = mutableMapOf<INode, List<NodeAnnotation<*>>>()

	fun <T> annotate(node: INode, value: T, tag: INodeAnnotationTag<T>, mergeOnConflict: Boolean = false) {
		val nAnnotation = NodeAnnotation(tag, value)

		val nAnnotations = when (val annotations = nodeAnnotationMap[node]) {
			null -> listOf(nAnnotation)
			else -> annotations + nAnnotation
		}

		nodeAnnotationMap[node] = nAnnotations
	}

	fun getAnnotations(node: INode) : List<NodeAnnotation<*>> = when (val annotations = nodeAnnotationMap[node]) {
		null -> emptyList()
		else -> annotations
	}

	fun removeAll(node: INode, tag: INodeAnnotationTag<*>) {
		val nAnnotations = getAnnotations(node)
			.filterNot { it.tag == tag }

		nodeAnnotationMap[node] = nAnnotations
	}
}

interface ScopedNode

interface INode {
	val firstToken: Token
	val lastToken: Token

	val id: String get() {
		return "${javaClass.simpleName}@${java.util.UUID.randomUUID()}"
	}
	val range: IntRange
		get() = IntRange(firstToken.position.absolute, lastToken.position.absolute)

	fun getChildren() : List<INode>

	fun <N: INode> search(nodeType: Class<N>, priorityComparator: PriorityComparator<N>? = null, ignoreScopedNodes: Boolean = false) : List<N> {
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
}

inline fun <reified T> INode.annotateByKey(value: T, tag: INodeAnnotationTag<T>, mergeOnConflict: Boolean = false) {
	val nodeAnnotationMap = getKoinInstance<NodeAnnotationMap>()

	if (mergeOnConflict) {
		nodeAnnotationMap.removeAll(this, tag)
	}

	nodeAnnotationMap.annotate(this, value, tag, mergeOnConflict)
}

inline fun <reified T> INode.annotateByKey(value: T, key: String, mergeOnConflict: Boolean = false) {
	annotateByKey(value, NodeAnnotationTag(key), mergeOnConflict)
}

inline fun <reified T> INode.getAnnotation(tag: INodeAnnotationTag<T>) : NodeAnnotation<T>? {
	val nodeAnnotationMap = getKoinInstance<NodeAnnotationMap>()

	val results = nodeAnnotationMap.getAnnotations(this)
		.filter { it.tag == tag }

	return when (results.size) {
		0 -> null
		1 -> results[0] as NodeAnnotation<T>
		else -> {
			if (results.all { it == results[0] }) {
				// If all the same, we've just accidentally annotated more than once with the same value
				return results[0] as NodeAnnotation<T>
			}

			throw Exception("Multiple annotations found for tag: $tag")
		}
	}
}

inline fun <reified T> INode.getAnnotationByKey(key: String) : NodeAnnotation<T>? {
	return getAnnotation(NodeAnnotationTag(key))
}

inline fun <reified N: INode> INode.search(priorityComparator: PriorityComparator<N>? = null, ignoreScopedNodes: Boolean = false) : List<N> {
	return search(N::class.java, ignoreScopedNodes = ignoreScopedNodes)
}

interface BoundNode : INode

fun INode.prettyPrintEmpty(depth: Int = 0) : String
	= "${" ".repeat(depth)}${javaClass.simpleName}"

fun INode.prettyPrintNonEmpty(depth: Int = 0) : String
	= "${" ".repeat(depth)}${javaClass.simpleName}\n${getChildren().joinToString("\n") { it.prettyPrint(depth + 1) }}"

fun INode.prettyPrint(depth: Int = 0) : String = when (getChildren().isEmpty()) {
	true -> prettyPrintEmpty(depth)
	else -> prettyPrintNonEmpty(depth)
}
