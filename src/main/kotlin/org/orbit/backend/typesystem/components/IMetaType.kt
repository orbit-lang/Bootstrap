package org.orbit.backend.typesystem.components

import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeCheckPosition
import org.orbit.core.nodes.INode
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

sealed interface IMetaType<M: IMetaType<M>> : Entity<M> {
    fun toBoolean() : Boolean = when (this) {
        is Always -> true
        is Never -> false
    }

    operator fun plus(other: IMetaType<*>) : IMetaType<*>
}

object Always : IMetaType<Always> {
    override val id: String = "Any"
    override fun substitute(substitution: Substitution): Always = this
    override fun plus(other: IMetaType<*>): IMetaType<*> = other
    override fun getCardinality(): ITypeCardinality = ITypeCardinality.Mono
    override fun equals(other: Any?): Boolean = true
    override fun toString(): String = "Any"
}

data class Never(val message: String, override val id: String = "!") : IMetaType<Never>, IArrow<Never> {
    override val effects: List<Effect> = listOf(Effect.die)

    fun panic(node: INode? = null): Nothing = when (node) {
        null -> throw getKoinInstance<Invocation>().make<TypeSystem>(message)
        else -> throw getKoinInstance<Invocation>().make<TypeSystem>(message, node)
    }

    override fun getTypeCheckPosition(): TypeCheckPosition
    = TypeCheckPosition.AlwaysRight

    override fun getDomain(): List<AnyType> = emptyList()
    override fun getCodomain(): AnyType = this
    override fun curry(): IArrow<*> = this
    override fun never(args: List<AnyType>): Never = this
    override fun getCardinality(): ITypeCardinality = ITypeCardinality.Mono

    override fun substitute(substitution: Substitution): Never = this
    override fun equals(other: Any?): Boolean = this === other
    operator fun plus(other: Never) : Never = Never("$message\n${other.message}")
    override fun plus(other: IMetaType<*>): IMetaType<*> = when (other) {
        is Always -> this
        is Never -> this + other
    }

    override fun prettyPrint(depth: Int): String {
        val printer = getKoinInstance<Printer>()

        return printer.apply(message, PrintableKey.Error)
    }

    override fun toString(): String = prettyPrint()
}

typealias AnyMetaType = IMetaType<*>