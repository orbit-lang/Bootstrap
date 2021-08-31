package org.orbit.graph.extensions

import org.orbit.core.nodes.KeyedNodeAnnotationTag
import org.orbit.core.nodes.Node
import org.orbit.core.nodes.NodeAnnotation
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.GraphEntity
import org.orbit.graph.components.ScopeIdentifier
import org.orbit.serial.Serial
import java.io.Serializable

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

inline fun <reified T> Node.annotate(value: T, key: Annotations, mergeOnConflict: Boolean = false) where T: Serial, T: Serializable {
    annotateByKey(value, key.key, mergeOnConflict)
}

fun Node.isAnnotated(key: Annotations) : Boolean = annotations.any {
    when (it.tag) {
        is KeyedNodeAnnotationTag<*> -> it.tag.key == key.key
        else -> false
    }
}

fun Node.remove(annotation: Annotations) {
    annotations.removeIf {
        when (it.tag) {
            is KeyedNodeAnnotationTag<*> -> it.tag.key == annotation.key
            else -> false
        }
    }
}

inline fun <reified T> Node.getAnnotation(key: Annotations) : NodeAnnotation<T>? where T: Serial, T: Serializable {
    return getAnnotationByKey(key.key)
}