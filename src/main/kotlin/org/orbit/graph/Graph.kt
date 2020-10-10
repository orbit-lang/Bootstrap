package org.orbit.graph

import org.json.JSONObject
import org.orbit.core.nodes.Node
import org.orbit.serial.Serial
import java.util.*

sealed class GraphEntity {
    enum class Direction {
        LTR, RTL
    }

    class Alias(name: String, id: Vertex.ID) : Vertex(name, id)

    // A single point in the dependency graph
    open class Vertex(val name: String, val id: ID = ID.random()) : GraphEntity() {
        data class ID(val uuid: UUID) : Serial {
            companion object {
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

class Graph {
    private val vertices = mutableSetOf<GraphEntity.Vertex>()
    private val edges = mutableSetOf<GraphEntity.Edge>()

    fun insert(name: String) : GraphEntity.Vertex.ID {
        val vertex = GraphEntity.Vertex(name)
        vertices.add(vertex)

        return vertex.id
    }

    fun alias(name: String, source: GraphEntity.Vertex.ID) {
        val sourceVertex = findVertex(source)
        if (sourceVertex.name == name) return

        vertices.add(GraphEntity.Alias(name, source))
    }

    fun findOrNull(id: GraphEntity.Vertex.ID) : GraphEntity.Vertex.ID? {
        return vertices.find { it.id == id }?.id
    }

    fun findVertex(name: String, preferShortest: Boolean) : GraphEntity.Vertex {
        return findVertex(find(name), preferShortest)
    }

    fun findVertex(id: GraphEntity.Vertex.ID, preferShortest: Boolean = false) : GraphEntity.Vertex {
        return when (val vertex = vertices.find { it.id == id }) {
            null -> throw Exception("Dependency not found: '$id'")
            is GraphEntity.Alias ->
                if (preferShortest) vertex
                else vertices.find { it.id == id && it.name != vertex.name }!!
            else -> vertex
        }
    }

    fun find(name: String) : GraphEntity.Vertex.ID {
        return vertices.find { it.name == name }?.id
            ?: throw Exception("Dependency not found: $name")
    }

    fun getName(id: GraphEntity.Vertex.ID) : String {
        return vertices.find { it.id == id }!!.name
    }

    fun link(left: GraphEntity.Vertex.ID, right: GraphEntity.Vertex.ID) : GraphEntity.Vertex.ID {
        val leftVertex = findVertex(left)
        val rightVertex = findVertex(right)

        if (leftVertex == rightVertex) {
            throw Exception("Dependency cycle detected: ${leftVertex.name} -> ${rightVertex.name}")
        }

        vertices.remove(rightVertex)

        val nVertex = GraphEntity.Vertex("${leftVertex.name}::${rightVertex.name}", rightVertex.id)

        vertices.add(nVertex)
        vertices.add(GraphEntity.Alias(rightVertex.name, nVertex.id))

        edges.add(GraphEntity.Edge(leftVertex.id, rightVertex.id))

        return nVertex.id
    }

    private fun edgesString() : String {
        val h = "Edges:"
        val e = edges.joinToString("\n") {
            val leftVertex = getName(it.left)
            val rightVertex = getName(it.right)

            "\t$leftVertex -> $rightVertex"
        }

        return """
        |$h
        |$e
        """.trimMargin()
    }

    override fun toString(): String {
        val uniqueVertices = vertices.distinctBy { it.id }
        val verticesString = uniqueVertices.map { vertex ->
            val aliases = vertices.filter { it.id == vertex.id }

            "Vertex: ${vertex.id}\n\t" + aliases.joinToString("\n\t")
        }.joinToString("\n")

        val v = "Vertices:\n\t${vertices.joinToString("\n\t") { it.name }}"
        return """
        |$verticesString
        |${edgesString()}
        """.trimMargin()
    }
}

fun Node.getGraphID() : GraphEntity.Vertex.ID {
    return getAnnotation<GraphEntity.Vertex.ID>(Annotations.GraphID)!!.value
}

fun Node.getGraphIDOrNull() : GraphEntity.Vertex.ID? {
    return getAnnotation<GraphEntity.Vertex.ID>(Annotations.GraphID)?.value
}