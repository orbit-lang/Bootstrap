package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.AlgebraicConstructorNode
import org.orbit.core.nodes.TypeDefNode

object TypeDefInference: ITypeInference<TypeDefNode, IMutableTypeEnvironment> {
    override fun infer(node: TypeDefNode, env: IMutableTypeEnvironment): AnyType {
        val path = node.getPath()

        val mType = when (node.body.count()) {
            0 -> Type(path)
            else -> {
                val lazyUnion = Lazy(path.toString(OrbitMangler)) {
                    val type = env.getTypeOrNull(path)?.component
                        ?: TODO("NOT A UNION")

                    when (type) {
                        is Union -> type
                        is TypeAlias -> type.type as Union
                        else -> TODO("NOT A UNION 2")
                    }
                }
                val nEnv = SelfTypeEnvironment(env.fork(), lazyUnion)

                nEnv.add(TypeAlias(path, lazyUnion))

                val constructorNodes = node.body.filterIsInstance<AlgebraicConstructorNode>()
                val constructors = TypeInferenceUtils.inferAllAs<AlgebraicConstructorNode, UnionConstructor>(constructorNodes, nEnv)
                val union = Union(constructors)

                TypeAlias(path, union)
            }
        }

        GlobalEnvironment.add(mType)

        return mType
    }
}