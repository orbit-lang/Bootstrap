package org.orbit.graph.extensions

import org.orbit.core.nodes.KeyedNodeAnnotationTag
import org.orbit.core.nodes.Node
import org.orbit.core.nodes.NodeAnnotation
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.GraphEntity
import org.orbit.graph.components.ScopeIdentifier
import org.orbit.serial.Serial

fun Node.getScopeIdentifier() : ScopeIdentifier {
    return getAnnotation<ScopeIdentifier>(Annotations.Scope)!!.value
}

fun Node.getScopeIdentifierOrNull() : ScopeIdentifier? {
    return getAnnotation<ScopeIdentifier>(Annotations.Scope)?.value
}

fun Node.getGraphID() : GraphEntity.Vertex.ID {
    return getAnnotation<GraphEntity.Vertex.ID>(Annotations.GraphID)!!.value
}

fun Node.getGraphIDOrNull() : GraphEntity.Vertex.ID? {
    return getAnnotation<GraphEntity.Vertex.ID>(Annotations.GraphID)?.value
}

inline fun <reified T: Serial> Node.annotate(value: T, key: Annotations, mergeOnConflict: Boolean = false) {
    annotateByKey(value, key.key, mergeOnConflict)
}

fun Node.remove(annotation: Annotations) {
    annotations.removeIf {
        when (it.tag) {
            is KeyedNodeAnnotationTag<*> -> it.tag.key == annotation.key
            else -> false
        }
    }
}

inline fun <reified T: Serial> Node.getAnnotation(key: Annotations) : NodeAnnotation<T>? {
    return getAnnotationByKey(key.key)
}