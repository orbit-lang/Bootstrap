package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.AlgebraicConstructorNode
import org.orbit.util.Invocation

object AlgebraicConstructorInference : ITypeInference<AlgebraicConstructorNode, ISelfTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: AlgebraicConstructorNode, env: ISelfTypeEnvironment): AnyType {
        val path = node.getPath()
        val union = env.getSelfType() as? IType.Lazy<IType.Union>
            ?: throw invocation.make<TypeSystem>("Cannot define Constructor $path for non-Union Type ${env.getSelfType()}", node)

        val parameters = when (node.parameters.count()) {
            0 -> listOf(IType.Unit)
            else -> TypeInferenceUtils.inferAll(node.parameters, env)
//            1 -> listOf(TypeInferenceUtils.infer(node.parameters[0], env))
//            else -> TODO("2+ Union Constructor args")
        }

        val constructor = IType.UnionConstructor(path.toString(OrbitMangler), union, parameters[0])

        GlobalEnvironment.add(IType.Alias(path, constructor))

        return constructor
    }
}