package org.orbit.core.nodes

import org.orbit.core.GraphEntity
import org.orbit.core.Path
import org.orbit.core.ScopeIdentifier

private const val KEY = "Orbit::Compiler::Graph::Annotations"

data class Annotations<T>(override val key: String) : INodeAnnotationTag<T> {
    companion object {
        val path = Annotations<Path>("$KEY::Path")
        val scope = Annotations<ScopeIdentifier>("$KEY::Scope")
        val graphId = Annotations<GraphEntity.Vertex.ID>("$KEY::GraphID")
        val resolved = Annotations<Boolean>("$KEY::Resolved")
        val index = Annotations<Int>("$KEY::Index")
        val deferFunction = Annotations<String>("$KEY::DeferFunction")
    }

    override fun equals(other: Any?): Boolean = when (other) {
        is INodeAnnotationTag<*> -> other.key == key
        else -> false
    }
}
