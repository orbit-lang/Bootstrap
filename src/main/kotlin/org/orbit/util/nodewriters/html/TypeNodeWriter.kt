package org.orbit.util.nodewriters.html

import org.orbit.core.nodes.TypeDefNode

object TypeNodeWriter : HtmlNodeWriter<TypeDefNode> {
    override fun write(node: TypeDefNode, depth: Int) : String {
        // TODO - getPath()
        val margin = depth * 16

        return "<br /><strong class='Keyword' style='margin-left: ${margin}px;'>Type(${node.typeIdentifierNode.value})</strong>"
    }
}