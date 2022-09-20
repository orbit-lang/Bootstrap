package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.ConstructorInvocationNode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.utils.TypeUtils
import org.orbit.util.Invocation

object ConstructorInvocationInference : ITypeInference<ConstructorInvocationNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: ConstructorInvocationNode, env: Env): IType<*> {
        val type = TypeSystemUtils.infer(node.typeExpressionNode, env)
        val constructableType = type as? IType.Type
            ?: throw invocation.make<TypeSystem>("Cannot construct value of uninhabited Type `${type.id}`", node.typeExpressionNode)

        val args = TypeSystemUtils.inferAll(node.parameterNodes, env)
        val params = env.getDeclaredMembers(type)

        if (args.count() != params.count()) throw invocation.make<TypeSystem>("Constructor for Type `${type.id}` expects ${params.count()} arguments, found ${args.count()}", node)

        args.zip(params).forEach {
            val result = TypeUtils.check(env, it.first, it.second)

            if (result is IType.Never) throw invocation.make<TypeSystem>(result.message, node)
        }

        return constructableType
    }
}