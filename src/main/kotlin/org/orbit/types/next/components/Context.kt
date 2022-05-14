package org.orbit.types.next.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.SourcePosition
import org.orbit.types.next.inference.InferenceUtil
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer

data class TypeVariable(
    override val fullyQualifiedName: String
) : DeclType {
    override val isSynthetic: Boolean = true
    override val kind: Kind = IntrinsicKinds.Type

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation = when (other) {
        is TypeVariable -> when (other.fullyQualifiedName == fullyQualifiedName) {
            true -> TypeRelation.Same(this, other)
            else -> TypeRelation.Unrelated(this, other)
        }

        else -> TypeRelation.Unrelated(this, other)
    }
}

data class Context(
    override val fullyQualifiedName: String,
    val typeVariables: List<TypeVariable>,
    val constraints: List<ITypeConstraint<TypeComponent>>
) : DeclType, KoinComponent {
    override val isSynthetic: Boolean = false
    override val kind: Kind = IntrinsicKinds.Context
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    fun check(inferenceUtil: InferenceUtil, given: List<TypeComponent>) : Boolean {
        if (given.count() != typeVariables.count()) throw invocation.make<TypeSystem>("Context ${toString(printer)}, declares ${typeVariables.count()} Type Variables, found ${given.count()}", SourcePosition.unknown)

        val subs = given.zip(typeVariables).map { Substitution(it.second, it.first) }
        val nConstraints = constraints.map {
            subs.fold(it) { acc, next ->
                acc.substitute(next.typeVariable, next.type)
            }
        }

        // TODO - Can we propagate the constraint results down through successive constraints, a la Kotlin's Smartcasts?
        val ctx = inferenceUtil.toCtx()

        nConstraints.forEach { it.check(ctx) }

        return true
    }

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation {
        TODO("Not yet implemented")
    }

    override fun toString(printer: Printer): String {
        val vars = typeVariables.joinToString(", ") { it.toString(printer) }

        return "${printer.apply(fullyQualifiedName, PrintableKey.Bold)} [$vars]"
    }
}

data class ContextInstantiation(val context: Context, val given: List<TypeComponent>) : TypeComponent, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override val fullyQualifiedName: String get() {
        val pretty = given.joinToString(", ") { it.fullyQualifiedName }
        return "(${context.fullyQualifiedName}) [$pretty]"
    }

    override val isSynthetic: Boolean = false
    override val kind: Kind = IntrinsicKinds.Context

    fun replace(parameter: AbstractTypeParameter, concrete: TypeComponent) : ContextInstantiation {
        val nGiven = given.map { when (it) {
            is AbstractTypeParameter -> when (it.fullyQualifiedName == parameter.fullyQualifiedName) {
                true -> concrete
                else -> it
            }

            else -> it
        }}

        return ContextInstantiation(context, nGiven)
    }

    fun instantiate(inferenceUtil: InferenceUtil) {
        if (given.count() != context.typeVariables.count()) throw invocation.make<TypeSystem>("Context ${toString(printer)}, declares ${context.typeVariables.count()} Type Variables, found ${given.count()}", SourcePosition.unknown)

        val subs = given.zip(context.typeVariables).map { Substitution(it.second, it.first) }
        val nConstraints = mutableListOf<ITypeConstraint<*>>()
        for (constraint in context.constraints) {
            var nConstraint = constraint
            for (sub in subs) {
                nConstraint = nConstraint.substitute(sub.typeVariable, sub.type)
            }

            nConstraints.add(nConstraint)
        }

        // TODO - Can we propagate the constraint results down through successive constraints, a la Kotlin's Smartcasts?
        val ctx = inferenceUtil.toCtx()

        nConstraints.forEach { it.check(ctx) }
    }

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation {
        TODO("")
    }
}
