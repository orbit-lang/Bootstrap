package org.orbit.graph.extensions

import org.orbit.core.AnySerializable
import org.orbit.core.nodes.*
import org.orbit.core.nodes.Annotations
import org.orbit.core.GraphEntity
import org.orbit.core.ScopeIdentifier
import org.orbit.core.SerialSignature
import org.orbit.types.next.components.Signature

fun Node.getScopeIdentifier() : ScopeIdentifier {
    return getAnnotation<ScopeIdentifier>(Annotations.Scope as NodeAnnotationTag<ScopeIdentifier>)!!.value
}

fun Node.getScopeIdentifierOrNull() : ScopeIdentifier? {
    return getAnnotation<ScopeIdentifier>(Annotations.Scope as NodeAnnotationTag<ScopeIdentifier>)?.value
}

fun Node.getGraphID() : GraphEntity.Vertex.ID {
    return getAnnotation<GraphEntity.Vertex.ID>(Annotations.GraphID as NodeAnnotationTag<GraphEntity.Vertex.ID>)!!.value
}

fun Node.getGraphIDOrNull() : GraphEntity.Vertex.ID? {
    return getAnnotation<GraphEntity.Vertex.ID>(Annotations.GraphID as NodeAnnotationTag<GraphEntity.Vertex.ID>)?.value
}

fun Node.getSignatureOrNull() : Signature?
    = getAnnotation(Annotations.Signature as NodeAnnotationTag<SerialSignature>)?.value?.signature

inline fun <reified T: AnySerializable> Node.annotate(value: T, key: Annotations, mergeOnConflict: Boolean = false) {
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

inline fun <reified T: AnySerializable> Node.getAnnotation(key: Annotations) : NodeAnnotation<T>? {
    return getAnnotationByKey(key.key)
}