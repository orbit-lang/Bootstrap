package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.components.Enum
import org.orbit.backend.typesystem.components.Unit
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.AnyArrow
import org.orbit.backend.typesystem.utils.EffectUtils
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.MethodCallNode
import org.orbit.util.Invocation

object MethodCallInference : ITypeInference<MethodCallNode, ITypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    private inline fun <reified T: IAccessibleType<String>> inferPropertyAccess(node: MethodCallNode, receiver: AnyType, env: ITypeEnvironment) : AnyType {
        val propertyName = node.messageIdentifier.identifier

        if (receiver is Enum) {
            val case = receiver.getCaseOrNull(propertyName)
                ?: throw invocation.make<TypeSystem>("Enum Type does not contain Case `$propertyName`", node)

            return case
        }

        if (receiver !is T) {
            throw invocation.make<TypeSystem>("Cannot access property `$propertyName` of non-Structural Type $receiver", node)
        }

        return receiver.access(propertyName)
    }

    override fun infer(node: MethodCallNode, env: ITypeEnvironment): AnyType {
        val type = TypeInferenceUtils.infer(node.receiverExpression, env)
        val receiver = when(env.getCurrentContext().isComplete()) {
            true -> env.getCurrentContext().applySpecialisations(type)
            else -> type
        }

        if (node.isPropertyAccess) return inferPropertyAccess<IAccessibleType<String>>(node, receiver.flatten(receiver, env), env)

        var args = TypeInferenceUtils.inferAll(node.arguments, env)
        var possibleArrows = env.getSignatures(node.messageIdentifier.identifier)
        val expected = (env as? AnnotatedTypeEnvironment)?.typeAnnotation
//            ?: (env as? AnnotatedSelfTypeEnvironment)?.typeAnnotation
            ?: Always

        if (possibleArrows.isEmpty()) {
            throw invocation.make<TypeSystem>("No methods found matching signature `$receiver.${node.messageIdentifier.identifier} : (${args.joinToString(", ")}) -> Any`", node)
        }

        if (possibleArrows.count() > 1) {
            // See if we can differentiate by return type
            possibleArrows = possibleArrows.filter { TypeUtils.checkEq(env, it.component.returns, expected) }
        }

        if (possibleArrows.isEmpty()) {
            throw invocation.make<TypeSystem>("No methods found matching signature `$receiver.${node.messageIdentifier.identifier} : (${args.joinToString(", ")}) -> Any`", node)
        }

        possibleArrows = possibleArrows.filter { TypeUtils.checkEq(env, receiver, it.component.receiver) }

        if (possibleArrows.count() > 1) {
            // See if we can find the most "specific" match (we could get here if a method was overloaded for a Trait AND an implementation of that Trait
            possibleArrows = possibleArrows.filter {
                receiver.getCanonicalName() == it.component.receiver.getCanonicalName() || TypeUtils.checkEq(env, receiver, it.component.receiver)
            }

            if (possibleArrows.count() > 1 && receiver is TypeVar) {
                possibleArrows = possibleArrows.filter {
                    val synth = ProofAssistant.synthesise(receiver, env)
                    val flat = it.component.receiver.flatten(Always, env)

                    synth == flat
                }
            }

            if (possibleArrows.count() > 1) {
                possibleArrows = possibleArrows.filter { it.component.parameters == args }

                val components = possibleArrows.map { it.component }
                    .distinct()

                if (components.count() > 1) {
                    possibleArrows = possibleArrows.filter { it.component.receiver == receiver }

                    if (possibleArrows.count() > 1) {
                        // We've failed to narrow down the results, we have to error now
                        throw invocation.make<TypeSystem>("Multiple methods found matching signature `${possibleArrows[0].component}`", node)
                    }
                }
            }
        }

        if (possibleArrows.isEmpty()) {
            throw invocation.make<TypeSystem>("No methods found matching signature `$receiver.${node.messageIdentifier.identifier} : (${args.joinToString(", ")}) -> *`", node)
        }

        val arrow = possibleArrows[0].component

        val argsCount = args.count()
        val paramsCount = arrow.parameters.count()

        if (argsCount != paramsCount) {
            // SPECIAL CASE - Allow `f() == f(Unit)`
            if (paramsCount == 1 && argsCount == 0 && arrow.parameters[0] == Unit) {
                args = listOf(Unit)
            } else {
                throw invocation.make<TypeSystem>(
                    "Method `${node.messageIdentifier.identifier}` expects $paramsCount arguments, found $argsCount",
                    node
                )
            }
        }

        val zip = args.zip(arrow.parameters)
        for ((idx, pair) in zip.withIndex()) {
            if (!TypeUtils.checkEq(env, pair.first, pair.second)) {
                throw invocation.make<TypeSystem>("Method `${node.messageIdentifier.identifier}` expects argument of Type `${pair.second}` at index $idx, found `${pair.first}`", node.arguments[idx])
            }
        }

        val nArrow = when (arrow.getUnsolvedTypeVariables().isEmpty()) {
            true -> arrow
            else -> {
                val allUnsolved = arrow.getUnsolvedTypeVariables()
                val substitutions = mutableListOf<Substitution>()
                for (tv in allUnsolved) {
                    val proof = ProofAssistant.resolve(tv, arrow, receiver, args)

                    if (proof is TypeVar) {
                        // We couldn't find any evidence of `tv` aliasing a concrete type,
                        // but if it has projections, we might be able to synthesise one
                        val synth = ProofAssistant.synthesise(proof, env)
                            ?: break

                        substitutions.add(Substitution(tv, synth))
                    } else {
                        substitutions.add(Substitution(tv, proof))
                    }
                }

                if (substitutions.count() != arrow.getUnsolvedTypeVariables().count()) {
                    val allOld = substitutions.map { it.old }
                    val unsolved = allUnsolved.filterNot { allOld.contains(it) }
                    val allPretty = unsolved.joinToString("\n\t")

                    throw invocation.make<TypeSystem>("Cannot infer the following Type Variables in the current context:\n\t$allPretty\n\nUse `within Ctx [T...]` to specialise expression", node)
                }

                substitutions.fold(arrow) { acc, next -> acc.substitute(next) }
            }
        }

        EffectUtils.check(nArrow, node.effectHandler, GlobalEnvironment)

        return nArrow.returns.flatten(Always, env)
    }
}

// TODO - Generalise & recurse
object ProofAssistant {
    fun synthesise(typeVariable: TypeVar, env: ITypeEnvironment) : Trait? {
        val projections = env.getProjections(typeVariable)

        return when (projections.count()) {
            0 -> null
            1 -> projections[0].component.target
            else -> TODO("Synthesising Types from multiple Traits is currently unsupported")
        }
    }

    fun resolve(typeVariable: TypeVar, arrow: AnyArrow, receiver: AnyType, args: List<AnyType>) : AnyType {
        val allArgs = listOf(receiver) + args

        for (param in arrow.getDomain().withIndex()) {
            if (param.value == typeVariable) return allArgs[param.index]

            when (val idx = param.value.getUnsolvedTypeVariables().indexOf(typeVariable)) {
                -1 -> continue
                else -> when (receiver) {
                    is Struct -> return receiver.members.getOrNull(idx)?.second ?: typeVariable
                    else -> continue
                }
            }
        }

        return typeVariable
    }

    fun resolve(typeVariable: TypeVar, signature: Signature, receiver: AnyType, args: List<AnyType>) : AnyType {
        if (signature.receiver == typeVariable) return receiver

        for (param in signature.parameters.withIndex()) {
            if (param.value == typeVariable) return args[param.index]
        }

        return when (val idx = signature.receiver.getUnsolvedTypeVariables().indexOf(typeVariable)) {
            -1 -> typeVariable
            else -> when (receiver) {
                is Struct -> receiver.members[idx].second
                else -> typeVariable
            }
        }
    }
}