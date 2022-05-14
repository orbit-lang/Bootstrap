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
) : DeclType, ITypeParameter {
    override val isSynthetic: Boolean = true
    override val kind: Kind = IntrinsicKinds.Type
    override val index: Int = -1

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

data class ContextInstantiation(val context: Next.Context, val given: List<TypeComponent>) : TypeComponent, KoinComponent {
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
        println("HERE")
    }

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation {
        TODO("")
    }
}

class Next {
    interface Constraint<Self: Constraint<Self>> : TypeComponent {
        fun sub(old: TypeComponent, new: TypeComponent) : Self
        fun solve(ctx: Ctx) : TypeComponent
        fun apply(inferenceUtil: InferenceUtil) : InternalControlType
    }

    data class Same(val a: TypeComponent, val b: TypeComponent) : Constraint<Same>, KoinComponent {
        override val fullyQualifiedName: String = "(${a.fullyQualifiedName} = ${b.fullyQualifiedName})"
        override val isSynthetic: Boolean = false
        override val kind: Kind = IntrinsicKinds.Context

        private val printer: Printer by inject()

        override fun sub(old: TypeComponent, new: TypeComponent) : Same {
            val nA = when (a.fullyQualifiedName) {
                old.fullyQualifiedName -> new
                else -> a
            }

            val nB = when (b.fullyQualifiedName) {
                old.fullyQualifiedName -> new
                else -> b
            }

            return Same(nA, nB)
        }

        override fun solve(ctx: Ctx): TypeComponent = when (AnyEq.eq(ctx, a, b)) {
            true -> b
            else -> Never("Equality Constraint does not hold `${a.toString(printer)} = ${b.toString(printer)}`")
        }

        override fun apply(inferenceUtil: InferenceUtil) : InternalControlType = when (b) {
            is ITypeParameter -> Never("Type ${a.toString(printer)} is not a concrete Type in this context")
            else -> {
                inferenceUtil.declare(Alias(a.fullyQualifiedName, b))

                Anything
            }
        }

        override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation {
            TODO("Not yet implemented")
        }
    }

    data class Context(override val fullyQualifiedName: String, val typeVariables: List<TypeComponent>, val constraints: List<Constraint<*>>) : DeclType, Constraint<Context>, KoinComponent {
        private val printer: Printer by inject()

        override val kind: Kind = IntrinsicKinds.Context
        override val isSynthetic: Boolean = false

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
}

fun <C: Next.Constraint<C>> C.sub(vararg subs: Pair<TypeComponent, TypeComponent>) : C
    = subs.fold(this) { acc, next -> acc.sub(next.first, next.second) }