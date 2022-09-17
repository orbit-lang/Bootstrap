package org.orbit.backend.typegen.components.walkers

import org.orbit.backend.typegen.utils.TypeGenUtil
import org.orbit.core.nodes.ModuleNode
import org.orbit.core.nodes.ProgramNode

object ProgramWalker : IPrecessNodeWalker<ProgramNode, org.orbit.precess.frontend.components.nodes.ProgramNode> {
    override fun walk(node: ProgramNode): org.orbit.precess.frontend.components.nodes.ProgramNode {
        val modules = TypeGenUtil.walkAll<ModuleNode, org.orbit.precess.frontend.components.nodes.ModuleNode>(node.getModuleDefs())

        return org.orbit.precess.frontend.components.nodes.ProgramNode(node.firstToken, node.lastToken, modules)
    }
}