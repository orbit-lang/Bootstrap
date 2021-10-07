package org.orbit.graph.components

import org.json.JSONObject
import org.orbit.serial.Serial
import java.io.Serializable

private const val KEY = "Orbit::Compiler::Graph::Annotations"

enum class Annotations(val key: String) {
    Path("$KEY::Path"),
    Scope("$KEY::Scope"),
    GraphID("$KEY::GraphID"),
    Type("$KEY::Type"),
    Resolved("$KEY::Resolved"),
    DeferFunction("$KEY::DeferFunction")
}

@JvmInline
value class StringKey(val value: String) : Serial, Serializable {
    override fun describe(json: JSONObject) {}
}