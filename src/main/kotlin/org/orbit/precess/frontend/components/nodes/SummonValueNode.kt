package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import java.lang.NullPointerException

data class SummonValueNode(override val firstToken: Token, override val lastToken: Token, val matchType: TypeExpressionNode<*>, val ref: RefLiteralNode) : DeclNode<Decl.Alias>() {
    override fun getChildren(): List<Node> = listOf(matchType, ref)
    override fun toString(): String = "summonValue $matchType as $ref"

    override fun getDecl(env: Env): DeclResult<Decl.Alias> {
        val matchType = when (val t = matchType.infer(env)) {
            is IType.Never -> return DeclResult.Failure(t)
            else -> t
        }

        val matches = env.refs.filter { it.type == matchType }

        return when (matches.count()) {
            0 -> DeclResult.Failure(IType.Never("Cannot summon value of Type `${matchType.id}` from current context: `$env`"))
            1 -> Decl.Alias(ref.refId, matches[0]).toSuccess()
            else -> DeclResult.Failure(IType.Never("Cannot summon value of Type `${matchType.id}` because multiple refs match in current context: `$env`"))
        }
    }
}
