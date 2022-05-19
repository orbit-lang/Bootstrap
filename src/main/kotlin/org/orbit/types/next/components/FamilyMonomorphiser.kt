package org.orbit.types.next.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.components.SourcePosition
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer

object FamilyMonomorphiser : Monomorphiser<PolymorphicType<TypeFamily<*>>, List<Pair<Int, TypeComponent>>, TypeFamily<*>>,
    KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun monomorphise(ctx: Ctx, input: PolymorphicType<TypeFamily<*>>, over: List<Pair<Int, TypeComponent>>, context: MonomorphisationContext): MonomorphisationResult<TypeFamily<*>> {
        if (over.count() > input.parameters.count()) return MonomorphisationResult.Failure(input.baseType)

        input.parameters.zip(over).forEach { parameters ->
            val omegaTrait = parameters.first.constraints.map { it.target }
                .mergeAll(ctx)

            if (omegaTrait !is Anything) {
                when (val result = omegaTrait.isImplemented(ctx, parameters.second.second)) {
                    is ContractResult.Failure -> throw invocation.make<TypeSystem>(result.getErrorMessage(printer, parameters.second.second),
                        SourcePosition.unknown
                    )
                    is ContractResult.Group -> when (result.isSuccessGroup) {
                        true -> {}
                        else -> throw invocation.make<TypeSystem>(result.getErrorMessage(printer, parameters.second.second),
                            SourcePosition.unknown
                        )
                    }

                    else -> {}
                }
            }
        }

        val nPath = OrbitMangler.unmangle(input.fullyQualifiedName).plus(over.map {
            OrbitMangler.unmangle(it.second.fullyQualifiedName)
        })

        val nFamily = TypeFamily<TypeComponent>(nPath, input.baseType.members)

        val nMembers = input.baseType.members.map {
            when (it) {
                is PolymorphicType<*> -> MonoUtil.monomorphise(ctx, it, over, nFamily).toType(printer)
                else -> throw invocation.make<TypeSystem>("Cannot monomorphise non-Polymorphic Type Family member ${it.toString(printer)}",
                    SourcePosition.unknown
                )
            }
        }

        return MonomorphisationResult.Total(TypeFamily(nPath, nMembers))
    }
}