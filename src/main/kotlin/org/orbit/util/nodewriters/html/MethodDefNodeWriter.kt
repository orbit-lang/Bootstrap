package org.orbit.util.nodewriters.html

import org.orbit.core.nodes.MethodDefNode

object MethodDefNodeWriter : HtmlNodeWriter<MethodDefNode> {
    override fun write(node: MethodDefNode, depth: Int): String {
        return MethodSignatureNodeWriter.write(node.signature, depth)
    }
}