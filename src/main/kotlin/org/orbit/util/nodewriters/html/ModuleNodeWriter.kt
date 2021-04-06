package org.orbit.util.nodewriters.html

import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.ModuleNode
import org.orbit.core.nodes.TypeDefNode

object ModuleNodeWriter : HtmlNodeWriter<ModuleNode> {
    override fun write(node: ModuleNode, depth: Int): String {
        val margin = depth * 16
        val path = node.getPath()

        var html = "<br /><strong class='Keyword' style='margin-left: ${margin}px;'>Module(${path.toString(OrbitMangler)}):</strong>"

        return html
    }
}