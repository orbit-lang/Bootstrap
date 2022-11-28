package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.ConstructorInvocationNode
import org.orbit.util.Invocation

object ConstructorInvocationInference : ITypeInference<ConstructorInvocationNode, IMutableTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: ConstructorInvocationNode, env: IMutableTypeEnvironment): AnyType {
        val args = TypeInferenceUtils.inferAll(node.parameterNodes, env)
        val nEnv = ConstructorTypeEnvironment(env, args)
        val type = TypeInferenceUtils.infer(node.typeExpressionNode, nEnv)
        var constructedType = type.flatten(type, nEnv) as? IType.IConstructableType<*>
            ?: throw invocation.make<TypeSystem>("Cannot construct value of uninhabited Type `$type`", node.typeExpressionNode)

        val typeVariables = constructedType.getUnsolvedTypeVariables()
        val subs = typeVariables.zip(args).map(::Substitution)

        val ctx = env.getTypeOrNull(type.getCanonicalName())?.context
        val constraints = when (ctx) {
            null -> emptyMap()
            else -> typeVariables.fold(emptyMap<AnyType, List<ITypeConstraint>>()) { acc, next ->
                val nType = subs.fold(next as AnyType) { acc, next -> acc.substitute(next) }
                val cons = ctx.getConstraints(next)
                val nCons = cons.map { subs.fold(it) { a, n -> a.substitute(n) as ITypeConstraint } }
                acc + (nType to nCons)
            }
        }

        constructedType = subs.fold(constructedType) { acc, next -> acc.substitute(next) as IType.IConstructableType<*> }

        for (pair in constraints) {
            val type = pair.key
            for (constraint in pair.value) {
                if (!constraint.isSolvedBy(type, nEnv)) {
                    throw invocation.make<TypeSystem>("Type $type does not satisfy Constraint $constraint", node)
                }
            }
        }

        val constructor = constructedType.getConstructor(args)
            ?: throw invocation.make<TypeSystem>("Type $constructedType does not declare a Constructor that accepts argument Types (${args.joinToString(", ")})", node)

        val params = constructor.getDomain()

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