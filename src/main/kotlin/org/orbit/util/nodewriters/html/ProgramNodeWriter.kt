package org.orbit.util.nodewriters.html

import org.orbit.core.nodes.ProgramNode

object ProgramNodeWriter : HtmlNodeWriter<ProgramNode> {
    override fun write(node: ProgramNode, depth: Int): String {
        return "<strong class='Neutral'>Program:</strong>"
    }
}