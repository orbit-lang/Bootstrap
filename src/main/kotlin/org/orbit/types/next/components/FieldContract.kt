package org.orbit.types.next.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.SourcePosition
import org.orbit.types.next.phase.TypeSystem
import org.orbit.types.next.utils.onlyOrNull
import org.orbit.util.Invocation
import org.orbit.util.Printer

data class FieldContract(override val trait: ITrait, override val input: Field) : Contract<Field>, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun isImplemented(ctx: Ctx, by: TypeComponent): ContractResult {
        if (by !is Type) return ContractResult.Failure(by, this)

        val baseMembers = by.getMembers()
        val projectedMembers = ctx.getTypes()
            .filterIsInstance<Projection>()
            .filter { NominalEq.eq(ctx, by, it.baseType) }
            .flatMap { it.projectedProperties }
            .map { it.project() }
            .filter { it is Field || it is Property }

        val allMembers = (baseMembers + projectedMembers)
            .distinctBy { it.memberName }

        if (allMembers.none { it.memberName == input.memberName }) return ContractResult.Failure(by, this)

        val match = allMembers.onlyOrNull { it.memberName == input.memberName && when (it) {
            is Field -> AnyEq.eq(ctx, input.type, it.type)
            is Property -> AnyEq.eq(ctx, input.type, it.lambda.returns)
            else -> TODO("!!!")
        }}

        return when (match) {
            null -> throw invocation.make<TypeSystem>(getErrorMessage(printer, by), SourcePosition.unknown)
            else -> ContractResult.Success(by, this)
        }
    }

    override fun substitute(old: TypeComponent, new: TypeComponent) : Contract<Field>
        = FieldContract(trait, MemberSubstituor.substitute(input, old, new) as Field)

    override fun matches(name: String): Boolean
        = input.memberName == name

    override fun getErrorMessage(printer: Printer, type: TypeComponent): String
        = "Type ${type.toString(printer)} does not implement Field ${input.toString(printer)} of Trait ${trait.toString(printer)}"
}
