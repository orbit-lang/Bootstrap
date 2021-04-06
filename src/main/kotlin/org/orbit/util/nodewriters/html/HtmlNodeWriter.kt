package org.orbit.util.nodewriters.html

import org.orbit.core.nodes.ModuleNode
import org.orbit.core.nodes.Node
import org.orbit.core.nodes.ProgramNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.util.nodewriters.NodeWriter
import org.orbit.util.nodewriters.NodeWriterFactory

interface HtmlNodeWriter<N: Node> : NodeWriter<N>

object HtmlNodeWriterFactory : NodeWriterFactory {
    override fun <N : Node> getNodeWriter(nodeClazz: Class<out N>, depth: Int): NodeWriter<N>? = when (nodeClazz) {
        ProgramNode::class.java -> ProgramNodeWriter as NodeWriter<N>
        ModuleNode::class.java -> ModuleNodeWriter as NodeWriter<N>
        TypeDefNode::class.java -> TypeNodeWriter as NodeWriter<N>
        else -> null
    }
}