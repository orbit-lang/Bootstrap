package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

data class ArrowNode(override val firstToken: Token, override val lastToken: Token, val domain: TypeExpressionNode<*>, val codomain: TypeExpressionNode<*>) : TypeExpressionNode<IType.Arrow1>() {
    override fun getChildren(): List<Node> = listOf(domain, codomain)
    override fun toString(): String = "($domain) -> $codomain"

    override fun infer(env: Env): IType.Arrow1 {
        val domainType = domain.infer(env)
        val codomainType = codomain.infer(env)

        return IType.Arrow1(domainType, codomainType)
    }
}
