package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.ast.DeclWalker
import org.orbit.precess.backend.ast.NodeWalker
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.phase.Interpreter

abstract class DeclNode<Self: DeclNode<Self>> : Node(), DeclWalker<Self>

data class TypeLiteralNode(override val firstToken: Token, override val lastToken: Token, val typeId: String) : DeclNode<TypeLiteralNode>() {
    override fun getChildren(): List<Node> = emptyList()
    override fun toString(): String = typeId

    override fun walk(interpreter: Interpreter): NodeWalker.WalkResult = NodeWalker.WalkResult.Success { env ->
        val t = IType.Type(typeId)
        val decl = Decl.Type(t, emptyMap())

        env.extend(decl)
    }
}
