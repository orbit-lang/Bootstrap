package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.AlgebraicConstructorNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.Expr
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.utils.AnyType

object AlgebraicConstructorInference : ITypeInference<AlgebraicConstructorNode> {
    override fun infer(node: AlgebraicConstructorNode, env: Env): AnyType {
        val path = node.getPath()
        // TODO - Allow recursive constructors
        val nType = IType.Type(path.toString(OrbitMangler))
        val members = node.parameters.map {
            val type = TypeSystemUtils.inferAs<TypeExpressionNode, AnyType>(it.typeExpressionNode, env)

            IType.Member(it.identifierNode.identifier, type, nType)
        }

        env.extendInPlace(Decl.Type(nType, members))

        return nType
    }
}

object TypeDefInference : ITypeInference<TypeDefNode> {
    override fun infer(node: TypeDefNode, env: Env): AnyType {
        val path = node.getPath()
        val type = IType.Type(path.toString(OrbitMangler))

        env.extendInPlace(Decl.Type(type, emptyMap()))

        val constructorNodes = node.body.filterIsInstance<AlgebraicConstructorNode>()
        val constructors = TypeSystemUtils.inferAllAs<AlgebraicConstructorNode, IType.Type>(constructorNodes, env)
        val decl = when (constructors.count()) {
            0 -> Decl.Type(type, emptyMap())
            1 -> Decl.Type(constructors[0], emptyMap())
            2 -> Decl.TypeAlias(path.toString(OrbitMangler), Expr.AnyTypeLiteral(IType.Union(constructors[0], constructors[1])))
            else -> TODO("Union > 3")
        }

        env.reduceInPlace(Decl.Type(type, emptyMap()))
        env.extendInPlace(decl)

        return type
    }
}