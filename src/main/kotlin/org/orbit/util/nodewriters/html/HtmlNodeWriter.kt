package org.orbit.util.nodewriters.html

import org.orbit.core.nodes.*
import org.orbit.util.nodewriters.NodeWriter
import org.orbit.util.nodewriters.NodeWriterFactory

interface HtmlNodeWriter<N: INode> : NodeWriter<N>

object HtmlNodeWriterFactory : NodeWriterFactory {
    override fun <N : INode> getNodeWriter(nodeClazz: Class<out N>, depth: Int): NodeWriter<N>? = when (nodeClazz) {
        ProgramNode::class.java -> ProgramNodeWriter
        ModuleNode::class.java -> ModuleNodeWriter
        TypeDefNode::class.java -> TypeNodeWriter
        MethodDefNode::class.java -> MethodDefNodeWriter
        else -> null
    } as? NodeWriter<N>
}