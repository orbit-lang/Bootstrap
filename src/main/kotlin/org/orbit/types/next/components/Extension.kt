package org.orbit.types.next.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.SourcePosition
import org.orbit.types.next.inference.InferenceUtil
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer
import java.util.UUID

fun Signature.sub(a: TypeComponent, b: TypeComponent) : Signature {
    val nReceiver = when (receiver.fullyQualifiedName) {
        a.fullyQualifiedName -> b
        else -> receiver
    }

    val nParams = parameters.map { when (it.fullyQualifiedName) {
        a.fullyQualifiedName -> b
        else -> it
    }}

    val nReturns = when (returns.fullyQualifiedName) {
        a.fullyQualifiedName -> b
        else -> returns
    }

    return Signature(relativeName, nReceiver, nParams, nReturns, false)
}

data class Extension(val extends: PolymorphicType<*>, val signatures: List<Signature>, val context: Context) : DeclType, KoinComponent {
    private val id = UUID.randomUUID()

    override val fullyQualifiedName: String get() {
        return "${extends.fullyQualifiedName} + ${context.fullyQualifiedName} @$id"
    }

    override val isSynthetic: Boolean = false
    override val kind: Kind = IntrinsicKinds.Extension

    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    fun extend(inferenceUtil: InferenceUtil, monomorphicType: MonomorphicType<TypeComponent>) : TypeComponent {
        if (monomorphicType.concreteParameters.count() != extends.parameters.count())
            throw invocation.make<TypeSystem>("TODO: Extension", SourcePosition.unknown)

        val nInferenceUtil = inferenceUtil.derive(self = extends)
        val nContext = extends.parameters.zip(monomorphicType.concreteParameters).fold(context) { acc, next ->
            acc.sub(next.first, next.second.concreteType)
        }

        return nContext.solve(nInferenceUtil.toCtx())
    }

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation {
        TODO("Not yet implemented")
    }
}

fun <A, B, R> List<A>.mapZip(other: List<B>, fn: (A, B) -> R) : List<R> {
    if (count() != other.count()) return emptyList()

    val nList = mutableListOf<R>()
    var ptr = 0
    while (ptr < count()) {
        val a = this[ptr]
        val b = other[ptr]

        nList.add(fn(a, b))

        ptr++
    }

    return nList
}