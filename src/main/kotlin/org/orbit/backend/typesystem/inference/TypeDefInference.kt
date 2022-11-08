package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.getPath
import org.orbit.core.nodes.AlgebraicConstructorNode
import org.orbit.core.nodes.TypeDefNode

object TypeDefInference: ITypeInference<TypeDefNode, IMutableTypeEnvironment> {
    override fun infer(node: TypeDefNode, env: IMutableTypeEnvironment): AnyType {
        val path = node.getPath()
        val nType = IType.Type(path)
        val nEnv = env.fork()

        nEnv.add(nType)

        val constructorNodes = node.body.filterIsInstance<AlgebraicConstructorNode>()
        val constructors = TypeInferenceUtils.inferAllAs<AlgebraicConstructorNode, IType.Type>(constructorNodes, nEnv)
        val mType = when (constructors.count()) {
            0 -> nType
            1 -> {
                env.add(constructors[0])
                constructors[0]
            }
            2 -> {
                env.add(constructors[0])
                env.add(constructors[1])

                IType.Alias(path, IType.Union(constructors[0], constructors[1]))
            }
            3 -> {
                env.add(constructors[0])
                env.add(constructors[1])
                env.add(constructors[2])

                IType.Alias(path, IType.Union(IType.Union(constructors[0], constructors[1]), constructors[2]))
            }
            else -> TODO("Union > 2")
        }

        env.add(mType)

        return mType
    }
}