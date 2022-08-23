package org.orbit.precess.backend.components

import org.orbit.precess.backend.utils.AnyType
import kotlin.reflect.KProperty

data class Env(
    val elements: List<IType<*>> = emptyList(),
    val refs: List<IRef> = emptyList(),
    val contracts: List<Contract> = emptyList(),
    val projections: List<Projection> = emptyList(),
    val expressionCache: Map<String, AnyType> = emptyMap()
) : IType<Env> {
    companion object {
        private var current: Env = Env()

        fun capture(fn: () -> Env): Env {
            current = fn()

            return current
        }

        operator fun getValue(obj: Any, property: KProperty<*>): Env = current
    }

    private sealed interface Protector<T> {
        object RefProtector : Protector<Ref?> {
            override fun protect(block: () -> Ref?): Ref? {
                val result = block() ?: return null

                return when (val t = result.type) {
                    is IType.Never -> panic(t)
                    else -> result
                }
            }
        }

        object TypeProtector : Protector<IType<*>?> {
            override fun protect(block: () -> IType<*>?): IType<*>? {
                val result = block() ?: return null

                return when (result) {
                    is IType.Never -> panic(result)
                    is IType.Member -> when (result.type) {
                        is IType.Never -> panic(result.type)
                        else -> result
                    }

                    else -> result
                }
            }
        }

        fun protect(block: () -> T): T
        fun <T> panic(never: IType.Never): T = never.panic()
    }

    internal constructor(type: IType<*>, ref: IRef) : this(listOf(type), listOf(ref))

    override val id: String = "∆"

    override fun substitute(substitution: Substitution): Env = this

    private fun <T> protect(protector: Protector<T>, block: () -> T): T = protector.protect(block)

    fun getRef(of: String): IRef? = protect(Protector.RefProtector) {
        refs.firstOrNull { it.name == of }
            // NOTE - A lookup for a defined Ref `r` bumps its use count
            ?.consume()
    }

    fun getElement(id: String): IType<*>? = protect(Protector.TypeProtector) {
        elements.firstOrNull { it.id == id }
    }

    inline fun <reified T : IType<T>> getElementAs(id: String): T? = getElement(id) as? T
    inline fun <reified T : IType<T>> getRefAs(of: String): T? = getRef(of) as? T

    fun getProjections(of: IType.Entity<*>): List<Projection> = projections.filter { it.source == of }

    fun getProjectedMembers(of: IType.Entity<*>): List<IType.Member> {
        val projections = getProjections(of)
            .map { it.target }
            .filterIsInstance<IType.ITrait.MembershipTrait>()

        return projections.flatMap { it.requiredMembers }
    }

    fun projects(source: IType.Entity<*>, target: IType.Entity<*>): Boolean =
        projections.any { it.source == source && it.target == target }

    fun getDeclaredMembers(of: IType.Type): List<IType.Member> = elements.filterIsInstance<IType.Member>()
        .filter { it.owner == of }

    fun getMembers(of: IType.Type): List<IType.Member> = getDeclaredMembers(of) + getProjectedMembers(of)

    fun extend(decl: Decl): Env
        = decl.extend(this)

    fun denyElement(id: String): Env {
        val nElements = elements.map {
            when (it.id) {
                id -> IType.Never("$id is not defined in the current Context", id)
                else -> it
            }
        }

        return Env(nElements, refs, contracts, projections, expressionCache)
    }

    fun denyRef(name: String): Env {
        val nRefs = refs.map {
            when (it.name) {
                name -> Ref(name, IType.Never("$name is not bound in the current Context", name))
                else -> it
            }
        }

        return Env(elements, nRefs, contracts, projections, expressionCache)
    }

    fun accept(contract: Contract): Env = Env(elements, refs, contracts + contract)
    fun verifyContracts() = contracts.forEach {
        when (val result = it.verify(this)) {
            is Contract.ContractResult.Verified -> {
            }
            is Contract.ContractResult.Violated -> result.reason.panic()
        }
    }

    operator fun plus(other: Env) : Env
        = other.extend(Decl.Merge(this))

    override fun toString(): String = when (elements.isEmpty()) {
        true -> "{}"
        else -> "{${elements.joinToString("; ") { it.id }}}"
    }
}