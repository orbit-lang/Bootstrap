package org.orbit.util.nodewriters.html

import org.orbit.core.nodes.MethodSignatureNode

object MethodSignatureNodeWriter : HtmlNodeWriter<MethodSignatureNode> {
    override fun write(node: MethodSignatureNode, depth: Int): String {
        val margin = 16 * depth
        return "<br /><strong class='Keyword' style='margin-left: ${margin}px;'>Method(${node.identifierNode.identifier})</strong>"
    }
}