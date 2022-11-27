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
            0 -> IType.Type(path)
            else -> {
                val lazyUnion = IType.Lazy(path.toString(OrbitMangler)) {
                    val type = env.getTypeOrNull(path)?.component
                        ?: TODO("NOT A UNION")

                    when (type) {
                        is IType.Union -> type
                        is IType.Alias -> type.type as IType.Union
                        else -> TODO("NOT A UNION 2")
                    }
                }
                val nEnv = SelfTypeEnvironment(env.fork(), lazyUnion)

                nEnv.add(IType.Alias(path, lazyUnion))

                val constructorNodes = node.body.filterIsInstance<AlgebraicConstructorNode>()
                val constructors = TypeInferenceUtils.inferAllAs<AlgebraicConstructorNode, IType.UnionConstructor>(constructorNodes, nEnv)
                val union = IType.Union(constructors)

                IType.Alias(path, union)
            }
        }

        GlobalEnvironment.add(mType)

        return mType
    }
}