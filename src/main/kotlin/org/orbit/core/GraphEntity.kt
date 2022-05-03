package org.orbit.core

import java.io.Serializable
import java.util.*

sealed class GraphEntity : AnySerializable() {
    class Alias(name: String, id: ID) : Vertex(name, id)

    // A single point in the dependency graph
    open class Vertex(val name: String, val id: ID = ID.random()) : GraphEntity(), Serializable {
        data class ID(val uuid: String) : AnySerializable(), Serializable {
            companion object {
                val Self = random()
                fun random() : ID = ID(UUID.randomUUID().toString())
            }

            override fun equals(other: Any?): Boolean = when (other) {
                is ID -> uuid == other.uuid
                else -> false
            }

            override fun toString(): String {
                return uuid
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
    class Edge(var left: Vertex.ID, var right: Vertex.ID) : GraphEntity(), Serializable {
        override fun toString(): String {
            return "$left -> $right"
        }
    }
}