package org.orbit.util.nodewriters

import org.orbit.core.nodes.Node

interface NodeWriter<N: Node> {
    fun write(node: N, depth: Int = 0) : String
}

interface NodeWriterFactory {
    fun <N: Node> getNodeWriter(nodeClazz: Class<out N>, depth: Int = 0) : NodeWriter<N>?
}

fun <N: Node> N.write(writerFactory: NodeWriterFactory, depth: Int) : String {
    val writer = writerFactory.getNodeWriter(this::class.java) ?: return ""
    val head = writer.write(this, depth)
    val sub = getChildren().joinToString("") { it.write(writerFactory, depth + 1) }

    return head + sub
}