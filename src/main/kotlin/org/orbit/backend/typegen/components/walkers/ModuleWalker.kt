package org.orbit.backend.typegen.components.walkers

import org.orbit.backend.typegen.utils.TypeGenUtil
import org.orbit.core.nodes.ContextNode
import org.orbit.core.nodes.MethodDefNode
import org.orbit.core.nodes.ModuleNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.precess.frontend.components.nodes.*

object ModuleWalker : IPrecessNodeWalker<ModuleNode, org.orbit.precess.frontend.components.nodes.ModuleNode> {
    override fun walk(node: ModuleNode): org.orbit.precess.frontend.components.nodes.ModuleNode {
        val contextProps = TypeGenUtil.walkAll<ContextNode, PropositionNode>(node.contexts)
        val typeProps = node.entityDefs.filterIsInstance<TypeDefNode>().map {
            val nNode = TypeGenUtil.walk<TypeDefNode, ModifyContextNode>(it)

            nNode.toPropositionNode("Mk${it.typeIdentifierNode.value}")
        }

        val arrowProps = node.methodDefs.map {
            val nNode = TypeGenUtil.walk<MethodDefNode, ModifyContextNode>(it)

            nNode.toPropositionNode("Mk${it.signature.identifierNode.identifier}")
        }

        val bodyProps = TypeGenUtil.walkAll<MethodDefNode, PropositionNode>(node.methodDefs)

        return ModuleNode(node.firstToken, node.lastToken, contextProps + typeProps + arrowProps + bodyProps)
    }
}