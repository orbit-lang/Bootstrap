package org.orbit.graph

import org.orbit.core.nodes.KeyedNodeAnnotationTag
import org.orbit.core.nodes.Node
import org.orbit.core.nodes.NodeAnnotation
import org.orbit.serial.Serial

private const val KEY = "Orbit::Compiler::Graph::Annotations"

enum class Annotations(val key: String) {
    Path("$KEY::Path"),
    Scope("$KEY::Scope"),
    GraphID("$KEY::GraphID")
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