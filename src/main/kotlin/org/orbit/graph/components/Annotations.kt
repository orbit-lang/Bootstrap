package org.orbit.graph.components

import org.orbit.core.nodes.KeyedNodeAnnotationTag
import org.orbit.core.nodes.Node
import org.orbit.core.nodes.NodeAnnotation
import org.orbit.serial.Serial

private const val KEY = "Orbit::Compiler::Graph::Annotations"

enum class Annotations(val key: String) {
    Path("$KEY::Path"),
    Scope("$KEY::Scope"),
    GraphID("$KEY::GraphID"),
    Type("$KEY::Type")
}