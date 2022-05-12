package org.orbit.core.nodes

import org.orbit.core.AnySerializable

private const val KEY = "Orbit::Compiler::Graph::Annotations"

sealed class Annotations(val key: String) : NodeAnnotationTag<AnySerializable>() {
    object Path : Annotations("$KEY::Path")
    object Scope : Annotations("$KEY::Scope")
    object GraphID : Annotations("$KEY::GraphID")
    object Type : Annotations("$KEY::Type")
    object Resolved : Annotations("$KEY::Resolved")
    object DeferFunction : Annotations("$KEY::DeferFunction")
    object Index : Annotations("$KEY::Index")
    object Signature : Annotations("$KEY::Signature")
}
