package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ConstructorTypeEnvironment
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.Substitution
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.ConstructorInvocationNode
import org.orbit.util.Invocation

object ConstructorInvocationInference : ITypeInference<ConstructorInvocationNode, ConstructorTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: ConstructorInvocationNode, env: ConstructorTypeEnvironment): AnyType {
        val args = TypeInferenceUtils.inferAll(node.parameterNodes, env)
        val nEnv = ConstructorTypeEnvironment(env, args)
        val type = TypeInferenceUtils.infer(node.typeExpressionNode, nEnv).flatten(nEnv)
        var constructedType = type as? IType.IConstructableType<*>
            ?: throw invocation.make<TypeSystem>("Cannot construct value of uninhabited Type `$type`", node.typeExpressionNode)

        val typeVariables = constructedType.getUnsolvedTypeVariables()
        val subs = typeVariables.zip(args).map(::Substitution)

        constructedType = subs.fold(constructedType) { acc, next -> acc.substitute(next) as IType.IConstructableType<*> }

        val params = constructedType.getConstructors()[0].getDomain()

        if (args.count() != params.count()) throw invocation.make<TypeSystem>("Constructor for Type `$type` expects ${params.count()} arguments, found ${args.count()}", node)

        val errors = mutableListOf<IType.Never>()
        args.zip(params).forEach {
            val result = TypeUtils.check(nEnv, it.first, it.second)

            if (result is IType.Never) errors.add(result)
        }

        if (errors.isNotEmpty()) {
            val header = "Cannot construct instance of Type `$constructedType` because the supplied arguments are mismatched in the following way(s):"
            val error = errors.joinToString("\n\t") { it.message }

            throw invocation.make<TypeSystem>("$header\n\t$error", node)
        }

        return constructedType
    }
}