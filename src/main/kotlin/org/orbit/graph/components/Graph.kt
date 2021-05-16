package org.orbit.graph.components

import org.orbit.core.OrbitMangler
import org.orbit.core.nodes.Node

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
        if (id == GraphEntity.Vertex.ID.Self) return GraphEntity.Vertex("Self", id)

        return when (val vertex = vertices.find { it.id == id }) {
            null -> throw Exception("Dependency not found: '$id'")
            is GraphEntity.Alias ->
                if (preferShortest) vertex
                else vertices.find { it.id == id && it.name != vertex.name }!!
            else -> vertex
        }
    }

    fun find(binding: Binding) : GraphEntity.Vertex.ID = when (binding) {
        Binding.Self -> GraphEntity.Vertex.ID.Self
        else -> find(binding.path.toString(OrbitMangler))
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