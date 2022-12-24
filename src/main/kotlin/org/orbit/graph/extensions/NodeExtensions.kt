package org.orbit.graph.extensions

import org.orbit.core.GraphEntity
import org.orbit.core.ScopeIdentifier
import org.orbit.core.nodes.*

fun INode.getScopeIdentifier() : ScopeIdentifier {
    return getAnnotation(Annotations.scope)!!.value
}

fun INode.getScopeIdentifierOrNull() : ScopeIdentifier? {
    return getAnnotation(Annotations.scope)?.value
}

fun INode.getGraphID() : GraphEntity.Vertex.ID {
    return getAnnotationByKey<GraphEntity.Vertex.ID>(Annotations.graphId.key)!!.value
}

fun INode.getGraphIDOrNull() : GraphEntity.Vertex.ID? {
    return getAnnotation(Annotations.graphId)?.value
}
