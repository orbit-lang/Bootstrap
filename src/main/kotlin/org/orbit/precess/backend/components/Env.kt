package org.orbit.precess.backend.components

import org.orbit.backend.typesystem.phase.IOrbModule
import org.orbit.backend.typesystem.phase.getPublicAPI
import org.orbit.core.nodes.OperatorFixity
import org.orbit.precess.backend.utils.AnyType
import kotlin.reflect.KProperty

class Env(
    val name: String = "",
    private var _elements: List<IType<*>> = emptyList(),
    private var _refs: List<IRef> = emptyList(),
    private var _contracts: List<Contract> = emptyList(),
    private var _projections: List<Projection> = emptyList(),
    private var _expressionCache: Map<String, AnyType> = emptyMap(),
    val parent: Env? = null
) : IType<Env> {
    companion object {
        private var current: Env = Env()

        fun capture(fn: () -> Env): Env {
            current = fn()

            return current
        }

        operator fun getValue(obj: Any, property: KProperty<*>): Env = current
    }

    val elements get() = _elements
    val refs get() = _refs
    val contracts get() = _contracts
    val projections get() = _projections
    val expressionCache get() = _expressionCache

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

    internal constructor(name: String = "", type: IType<*>, ref: IRef) : this(name, listOf(type), listOf(ref))

    override val id: String get() {
        return "$name : ${toString()}"
    }

    override fun substitute(substitution: Substitution): Env = this

    private fun <T> protect(protector: Protector<T>, block: () -> T): T = protector.protect(block)

    fun getRef(of: String): IRef? = protect(Protector.RefProtector) {
        refs.firstOrNull { it.name == of }
            // NOTE - A lookup for a defined Ref `r` bumps its use count
            ?.consume()
    }

    fun contains(type: AnyType) : AnyType
        = type.exists(this)

    fun getElement(id: String): IType<*>? = protect(Protector.TypeProtector) {
        elements.firstOrNull { it.getCanonicalName() == id }
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

    fun extendAll(decls: List<Decl>) : Env
        = decls.fold(this) { acc, next -> acc.extend(next) }

    fun extendInPlace(decl: Decl) {
        val nEnv = decl.extend(this)

        _elements = nEnv.elements
        _refs = nEnv.refs
        _contracts = nEnv.contracts
        _projections = nEnv.projections
        _expressionCache = nEnv.expressionCache
    }

    fun reduceInPlace(decl: Decl) {
        val nEnv = decl.reduce(this)

        _elements = nEnv.elements
        _refs = nEnv.refs
        _contracts = nEnv.contracts
        _projections = nEnv.projections
        _expressionCache = nEnv.expressionCache
    }

    fun reduce(decl: Decl) : Env
        = decl.reduce(this)

    fun reduceAll(decls: List<Decl>) : Env
        = decls.fold(this) { acc, next -> acc.reduce(next) }

    fun manage(decl: Decl, block: (Env) -> Unit) {
        extendInPlace(decl)
        block(this)
        reduceInPlace(decl)
    }

    fun withSelf(type: AnyType) : Env
        = extend(Decl.TypeAlias("Self", Expr.AnyTypeLiteral(type)))

    fun withSelf(type: AnyType, block: (Env) -> Unit)
        = manage(Decl.TypeAlias("Self", Expr.AnyTypeLiteral(type)), block)

    fun denyElement(id: String): Env {
        val nElements = elements.map {
            when (it.id) {
                id -> IType.Never("$id is not defined in the current Context", id)
                else -> it
            }
        }

        return Env(name, nElements, refs, contracts, projections, expressionCache)
    }

    fun denyRef(name: String): Env {
        val nRefs = refs.map {
            when (it.name) {
                name -> Ref(name, IType.Never("$name is not bound in the current Context", name))
                else -> it
            }
        }

        return Env(name, elements, nRefs, contracts, projections, expressionCache)
    }

    fun accept(contract: Contract): Env = Env(name, elements, refs, contracts + contract)
    fun verifyContracts() = contracts.forEach {
        when (val result = it.verify(this)) {
            is Contract.ContractResult.Verified -> {
            }
            is Contract.ContractResult.Violated -> result.reason.panic()
        }
    }

    inline fun <reified O: IType.IOperatorArrow<*, *>> getOperators() : List<O>
        = elements.filterIsInstance<IType.Alias>()
            .map { it.type }
            .filterIsInstance<O>()

    fun import(module: IOrbModule) : Env
        = Decl.Merge(module.getPublicAPI()).extend(this)

    fun importInPlace(module: IOrbModule) {
        val nEnv = import(module)

        _elements = nEnv.elements
        _refs = nEnv.refs
        _contracts = nEnv.contracts
        _projections = nEnv.projections
        _expressionCache = nEnv.expressionCache
    }

    operator fun plus(other: Env) : Env
        = other.extend(Decl.Merge(this))

    private fun prettyPrint() : String {
        val allTypes = elements.joinToString(", ") { it.id }
        val allRefs = refs.joinToString(", ")

        return "{$allTypes ; $allRefs}"
    }

    override fun exists(env: Env): AnyType = this

    override fun toString(): String = when (elements.isEmpty()) {
        true -> "{}"
        else -> prettyPrint()
    }
}