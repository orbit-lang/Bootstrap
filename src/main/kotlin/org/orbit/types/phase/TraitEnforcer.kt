package org.orbit.types.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.phase.AdaptablePhase
import org.orbit.types.components.*
import org.orbit.types.util.SignatureSelfSpecialisation
import org.orbit.util.*

class TraitConstraintEnforcer<T: TypeProtocol>(private val trait: Trait, private val type: Entity, private val componentGenerator: () -> List<T>, private val constraintGenerator: () -> List<Constraint<T, Entity>>) {
    fun enforce(context: ContextProtocol) : TraitEnforcementResult<T> {
        if (componentGenerator().isEmpty()) return TraitEnforcementResult.SuccessGroup(emptyList())

        var result: TraitEnforcementResult<T> = TraitEnforcementResult.None()
        val constraints = constraintGenerator()

        for (constraint in constraints) {
            result = when (constraint.checkConformance(context, type)) {
                true -> result.plus(TraitEnforcementResult.Exists(constraint.target))
                else -> result.plus(TraitEnforcementResult.Missing(type, trait, constraint.target))
            }
        }

        return result.promote()
    }
}

class TraitEnforcer(private val isImplicitConformance: Boolean = false) : AdaptablePhase<Context, Context>(), KoinComponent {
    override val inputType: Class<Context> = Context::class.java
    override val outputType: Class<Context> = Context::class.java

    override val invocation: Invocation by inject()
    private val printer: Printer by inject()

    fun enforce(context: ContextProtocol, type: Type) {
        var propertiesResult: TraitEnforcementResult<Property> = TraitEnforcementResult.None()
        for (trait in type.traitConformance) {
            val enforcer = TraitConstraintEnforcer(trait, type, trait::properties, trait::buildPropertyConstraints)

            propertiesResult += enforcer.enforce(context)
        }

        if (propertiesResult is TraitEnforcementResult.FailureGroup) {
            throw invocation.error<TraitEnforcer>(TraitEnforcerPropertyErrors(propertiesResult, isImplicitConformance))
        }

        var signaturesResult: TraitEnforcementResult<SignatureProtocol<*>> = TraitEnforcementResult.None()
        for (trait in type.traitConformance) {
            val enforcer = TraitConstraintEnforcer(trait, type, trait::signatures) {
                val sigs = (trait.signatures as List<TypeSignature>)
                    .map { SignatureSelfSpecialisation(it, type).specialise(context) }

                sigs.map { SignatureConstraint(trait, it) }
            }

            signaturesResult += enforcer.enforce(context)
        }

        if (signaturesResult is TraitEnforcementResult.FailureGroup) {
            // TODO - Proper error message
            val sigs = signaturesResult.results.map {
                when (it) {
                    is TraitEnforcementResult.Missing -> {
                        val specialiser = SignatureSelfSpecialisation(it.value as TypeSignature, type)
                        val mono = specialiser.specialise(context)

                        mono.toString(printer)
                    }
                    else -> ""
                }
            }

            val msg = sigs.joinToString("\n\t\t")

            throw invocation.make("Required signature(s) are unimplemented for type ${type.toString(printer)}\n\t\t$msg")
        }
    }

    private fun enforceAll(context: Context, module: Module) {
        /**
         * The rules here are simple for now; if a Type A declares conformance to a Trait B, then:
         *      1. A's set of declared properties must contain AT LEAST all of those declared by B; and
         *      2. A must implement all methods declared in B
         */
        module.entities.filterIsInstance<Type>()
            .forEach(partialReverse(::enforce, context))
    }

    override fun execute(input: Context): Context {
        input.types.filterIsInstance<Module>()
            .forEach(partialReverse(::enforceAll, input))

        return input
    }
}