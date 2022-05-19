package org.orbit.types.next.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.types.next.inference.InferenceUtil
import org.orbit.util.Printer
import org.orbit.util.Result

data class Context(override val fullyQualifiedName: String, val typeVariables: List<TypeComponent>, val constraints: List<Constraint<*>>) : DeclType, Constraint<Context>, KoinComponent {
    companion object {
        val empty = Context("", emptyList(), emptyList())
    }

    private val printer: Printer by inject()

    override val kind: Kind = IntrinsicKinds.Context
    override val isSynthetic: Boolean = false

    fun merge(other: Context): Result<Context, Never> {
        val conflicts = conflicts(other)
        val nTypeVariables = (typeVariables + other.typeVariables).distinctBy { it.fullyQualifiedName }

        if (conflicts is Anything) return Result.Success(Context("$fullyQualifiedName & ${other.fullyQualifiedName}", nTypeVariables, constraints + other.constraints))

        return Result.Failure(conflicts as Never)
    }

    override fun conflicts(with: Constraint<*>): InternalControlType = when (with) {
        is Context -> {
            var conflicts: InternalControlType = Anything
            for (a in constraints) {
                for (b in with.constraints) {
                    conflicts += a.conflicts(b)
                }
            }

            conflicts
        }

        else -> Anything
    }

    override fun sub(old: TypeComponent, new: TypeComponent): Context
        = Context(fullyQualifiedName, typeVariables, constraints.map { it.sub(old, new) })

    override fun solve(ctx: Ctx): TypeComponent {
        val failures = mutableListOf<Never>()
        for (constraint in constraints) {
            val result = constraint.solve(ctx)

            if (result is Never) failures.add(result)
        }

        return when (failures.isEmpty()) {
            true -> this
            else -> failures.combine("Context ${toString(printer)} contains the following broken constraints:")
        }
    }

    override fun apply(inferenceUtil: InferenceUtil): InternalControlType
        = constraints.fold(Anything as InternalControlType) { acc, next -> acc + next.apply(inferenceUtil) }

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation {
        TODO("Not yet implemented")
    }
}