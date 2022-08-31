package org.orbit.graph.extensions

import org.orbit.core.AnySerializable
import org.orbit.core.GraphEntity
import org.orbit.core.ScopeIdentifier
import org.orbit.core.nodes.*

fun INode.getScopeIdentifier() : ScopeIdentifier {
    return getAnnotation(Annotations.Scope as NodeAnnotationTag<ScopeIdentifier>)!!.value
}

fun INode.getScopeIdentifierOrNull() : ScopeIdentifier? {
    return getAnnotation(Annotations.Scope as NodeAnnotationTag<ScopeIdentifier>)?.value
}

fun INode.getGraphID() : GraphEntity.Vertex.ID {
    return getAnnotation(Annotations.GraphID as NodeAnnotationTag<GraphEntity.Vertex.ID>)!!.value
}

fun INode.getGraphIDOrNull() : GraphEntity.Vertex.ID? {
    return getAnnotation(Annotations.GraphID as NodeAnnotationTag<GraphEntity.Vertex.ID>)?.value
}

inline fun <reified T: AnySerializable> INode.annotateByKey(value: T, key: Annotations, mergeOnConflict: Boolean = false) {
    annotateByKey(value, key.key, mergeOnConflict)
}

// TODO
//fun INode.remove(annotation: Annotations) {
//
//
//    annotations.removeIf {
//        when (it.tag) {
//            is KeyedNodeAnnotationTag<*> -> it.tag.key == annotation.key
//            else -> false
//        }
//    }
//}

inline fun <reified T: AnySerializable> INode.getAnnotation(key: Annotations) : NodeAnnotation<T>? {
    return getAnnotationByKey(key.key)
}