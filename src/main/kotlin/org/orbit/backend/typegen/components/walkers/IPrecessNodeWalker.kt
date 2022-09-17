package org.orbit.backend.typegen.components.walkers

import org.orbit.core.nodes.INode
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Expr
import org.orbit.precess.frontend.components.nodes.*

interface IPrecessNodeWalker<N: INode, P: IPrecessNode> {
    fun walk(node: N) : P
}

interface IDeclWalker<N: INode, D: Decl> : IPrecessNodeWalker<N, DeclNode<D>>
interface IExprWalker<N: INode, E: Expr<E>> : IPrecessNodeWalker<N, TermExpressionNode<E>>
interface IStatementWalker<N: INode, S: IStatementNode> : IPrecessNodeWalker<N, S>
interface ITypeWalker<N: INode> : IPrecessNodeWalker<N, TypeExpressionNode>
interface IPropositionWalker<N: INode, P: PropositionExpressionNode> : IPrecessNodeWalker<N, P>