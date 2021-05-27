package org.orbit.graph.components

import org.json.JSONObject
import org.orbit.serial.Serial
import java.io.Serializable
import java.util.*

sealed class GraphEntity : Serializable {
    class Alias(name: String, id: ID) : Vertex(name, id)

    // A single point in the dependency graph
    open class Vertex(val name: String, val id: ID = ID.random()) : GraphEntity() {
        data class ID(val uuid: UUID) : Serial, Serializable {
            companion object {
                val Self = random()
                fun random() : ID = ID(UUID.randomUUID())
            }

            override fun equals(other: Any?): Boolean = when (other) {
                is ID -> uuid == other.uuid
                else -> false
            }

            override fun toString(): String {
                return uuid.toString()
            }

            override fun describe(json: JSONObject) {
                json.put("vertex.id", uuid.toString())
            }
        }

        override fun equals(other: Any?): Boolean = when (other) {
            is Vertex -> id == other.id && name == other.name
            else -> false
        }

        override fun toString(): String {
            return name
        }
    }

    // A link between two entities representing a dependency
    class Edge(var left: Vertex.ID, var right: Vertex.ID) : GraphEntity() {
        override fun toString(): String {
            return "$left -> $right"
        }
    }
}