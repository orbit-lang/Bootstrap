package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Decl
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.utils.TypeSystemUtilsOLD
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.AlgebraicConstructorNode
import org.orbit.core.nodes.TypeDefNode

object AlgebraicConstructorInferenceOLD : ITypeInferenceOLD<AlgebraicConstructorNode> {
    override fun infer(node: AlgebraicConstructorNode, env: Env): AnyType {
        val path = node.getPath()
        // TODO - Allow recursive constructors
        // TODO - Allow Structs, Tuples, Aliases
        val nType = IType.Type(path.toString(OrbitMangler))

        env.extendInPlace(Decl.Type(nType))

        return nType
    }
}

object TypeDefInferenceOLD : ITypeInferenceOLD<TypeDefNode> {
    override fun infer(node: TypeDefNode, env: Env): AnyType {
        val path = node.getPath()
        val type = IType.Type(path.toString(OrbitMangler))

        env.extendInPlace(Decl.Type(type))

        val constructorNodes = node.body.filterIsInstance<AlgebraicConstructorNode>()
        val constructors = TypeSystemUtilsOLD.inferAllAs<AlgebraicConstructorNode, IType.Type>(constructorNodes, env)
        val decl = when (constructors.count()) {
            0 -> Decl.Type(type)
            1 -> Decl.Type(constructors[0])
            2 -> Decl.TypeAlias(path.toString(OrbitMangler), IType.Union(constructors[0], constructors[1]))
            else -> TODO("Union > 3")
        }

        env.reduceInPlace(Decl.Type(type))
        env.extendInPlace(decl)

        return type
    }
}

