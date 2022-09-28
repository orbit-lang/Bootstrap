package org.orbit.backend.typegen.components.walkers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typegen.phase.TypeGen
import org.orbit.backend.typegen.utils.TypeGenUtil
import org.orbit.core.nodes.*
import org.orbit.backend.typesystem.components.ContextOperator
import org.orbit.precess.backend.components.Expr
import org.orbit.precess.frontend.components.nodes.*
import org.orbit.precess.frontend.components.nodes.TypeAliasNode
import org.orbit.util.Invocation

object SumConstructorWalker : IExprWalker<AlgebraicConstructorNode, Expr.Type> {
    override fun walk(node: AlgebraicConstructorNode): TermExpressionNode<Expr.Type>
        = EntityLookupNode(node.firstToken, node.lastToken, ContextLiteralNode.root, node.typeIdentifier.value)
}

object TypeDefWalker : IPropositionWalker<TypeDefNode, ModifyContextNode>, KoinComponent {
    private val invocation: Invocation by inject()

    private fun walkAlgebraicType(node: TypeDefNode, decl: TypeLiteralNode, constructors: List<TermExpressionNode<Expr.Type>>) : ModifyContextNode {
        if (constructors.count() != 2) throw invocation.make<TypeGen>("Algebraic Type must have exactly 2 Constructors (TODO)", node)

        val nDecl = when (constructors.isEmpty()) {
            true -> decl
            else -> {
                // TODO - Allow for arbitrary number of constructor cases
                TypeAliasNode(node.firstToken, node.lastToken, decl.typeId, SumTypeExpressionNode(node.firstToken, node.lastToken, constructors[0], constructors[1]))
            }
        }

        return ModifyContextNode(node.firstToken, node.lastToken, ContextLiteralNode.root, nDecl, ContextOperator.Extend)
    }

    override fun walk(node: TypeDefNode): ModifyContextNode {
        val constructors = TypeGenUtil.walkAll<ITypeDefBodyNode, TermExpressionNode<Expr.Type>>(node.body)
        val decl = TypeLiteralNode(node.firstToken, node.lastToken, node.typeIdentifierNode.value)

        return when (constructors.isEmpty()) {
            true -> ModifyContextNode(node.firstToken, node.lastToken, ContextLiteralNode.root, decl, ContextOperator.Extend)
            else -> walkAlgebraicType(node, decl, constructors)
        }
    }
}

object AnyEntityDefWalker : IPropositionWalker<EntityDefNode, ModifyContextNode> {
    override fun walk(node: EntityDefNode): ModifyContextNode = when (node) {
        is TypeDefNode -> TypeDefWalker.walk(node)
        is TraitDefNode -> TODO()
        is FamilyNode -> TODO()
    }
}