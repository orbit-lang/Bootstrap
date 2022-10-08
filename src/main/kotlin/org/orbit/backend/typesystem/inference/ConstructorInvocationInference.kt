package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.Substitution
import org.orbit.backend.typesystem.inference.evidence.asSuccessOrNull
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.ConstructorInvocationNode
import org.orbit.util.Invocation

object ConstructorInvocationInference : ITypeInference<ConstructorInvocationNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: ConstructorInvocationNode, env: Env): AnyType {
        val args = TypeSystemUtils.inferAll(node.parameterNodes, env)

        TypeSystemUtils.pushConstructorArgsAnnotation(args)

        val type = TypeSystemUtils.infer(node.typeExpressionNode, env).flatten(env)
        var constructableType = type as? IType.IConstructableType<*>
            ?: throw invocation.make<TypeSystem>("Cannot construct value of uninhabited Type `$type`", node.typeExpressionNode)

        val typeVariables = constructableType.getUnsolvedTypeVariables()
        val subs = typeVariables.zip(args).map(::Substitution)
        constructableType = subs.fold(constructableType) { acc, next -> acc.substitute(next) as IType.IConstructableType<*> }

        val params = constructableType.getConstructors()[0].getDomain()

        if (args.count() != params.count()) throw invocation.make<TypeSystem>("Constructor for Type `$type` expects ${params.count()} arguments, found ${args.count()}", node)

        val errors = mutableListOf<IType.Never>()
        args.zip(params).forEach {
            val result = TypeUtils.check(env, it.first, it.second)

            if (result is IType.Never) errors.add(result)
        }

        if (errors.isNotEmpty()) {
//            println()
            val header = "Cannot construct instance of Type `$constructableType` because the supplied arguments are mismatched in the following way(s):"
            val error = errors.joinToString("\n\t") { it.message }

            throw invocation.make<TypeSystem>("$header\n\t$error", node)
        }

        return constructableType
    }
}