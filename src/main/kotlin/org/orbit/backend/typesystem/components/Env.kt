package org.orbit.backend.typesystem.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.inference.evidence.ContextualEvidence
import org.orbit.backend.typesystem.intrinsics.IOrbModule
import org.orbit.backend.typesystem.intrinsics.getPublicAPI
import org.orbit.backend.typesystem.phase.globalContext
import org.orbit.backend.typesystem.utils.AnyArrow
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance
import kotlin.math.exp

interface IContextualComponent

class Env(
    val name: String = "",
    private var _elements: List<AnyType> = emptyList(),
    private var _refs: List<IRef> = emptyList(),
    private var _projections: List<Projection> = emptyList(),
    private var _expressionCache: Map<String, AnyType> = emptyMap(),
    val context: Context,
    val components: List<String> = listOf(name)
) : AnyType {
    constructor() : this("\uD835\uDF92", context = Context())

    companion object : KoinComponent {
        private val globalContext: Env by globalContext()

        fun findEvidence(elementName: String) : ContextualEvidence? {
            // First, check the easiest case where `elementName` is visible in the global context
            val element = globalContext.getElement(elementName)

            if (element != null) return ContextualEvidence(globalContext)

            // No such luck! Let's start iterating over child contexts (seeing as how we know we're currently in global)
            for (elem in globalContext.elements) {
                if (elem !is Env) continue
                val type = elem.getElement(elementName)

                if (type != null) return ContextualEvidence(elem)
            }

            // If we didn't find our element in any visible context, it should mean the element is actually undefined
            return null
        }
    }

    val elements get() = _elements
    val refs get() = _refs
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

        object TypeProtector : Protector<AnyType?> {
            override fun protect(block: () -> AnyType?): AnyType? {
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

    internal constructor(name: String = "", type: AnyType, ref: IRef) : this(name, listOf(type), listOf(ref), context = Context())

    override val id: String get() {
        return "$name : ${toString()}"
    }

    override fun getCanonicalName(): String = name

    override fun getCardinality(): ITypeCardinality
        = ITypeCardinality.Zero

    override fun substitute(substitution: Substitution): Env
        = Env(name, elements.map { it.substitute(substitution) }, refs, projections, expressionCache, context.substitute(substitution))

    private fun <T> protect(protector: Protector<T>, block: () -> T): T = protector.protect(block)

    fun solving(typeVariable: IType.TypeVar, concrete: AnyType) : Env {
        val substitution = Substitution(typeVariable, concrete)
        val nElements = (elements.filterNot { it == typeVariable } + IType.Alias(typeVariable.name, concrete))
            .substitute(substitution)

        return Env(name, nElements, refs.substitute(substitution), projections, expressionCache, context.substitute(substitution))
    }

    fun solvingAll(pairs: List<Pair<IType.TypeVar, AnyType>>) : Env
        = pairs.fold(this) { acc, next -> acc.solving(next.first, next.second) }

    fun getUnsolvedTypeParameters() : List<IType.TypeVar>
        = elements.filterIsInstance<IType.TypeVar>()

    fun getRef(of: String): IRef? = protect(Protector.RefProtector) {
        refs.firstOrNull { it.name == of }
            // NOTE - A lookup for a defined Ref `r` bumps its use count
            ?.consume()
    }

    fun contains(type: AnyType) : AnyType
        = type.exists(this)

    fun getElement(id: String): AnyType? = protect(Protector.TypeProtector) {
        elements.firstOrNull { it.getCanonicalName() == id }
    }

    inline fun <reified T : AnyType> getElementAs(id: String): T? = getElement(id) as? T
    inline fun <reified T : AnyType> getRefAs(of: String): T? = getRef(of) as? T

    fun getProjections(of: AnyType): List<Projection> = projections.filter { it.source == of }

    fun getProjectedMembers(of: IType.Entity<*>): List<IType.Member> {
        val projections = getProjections(of)
            .map { it.target }
            .filterIsInstance<IType.Trait>()

        return projections.flatMap { it.members }
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
        _projections = nEnv.projections
        _expressionCache = nEnv.expressionCache
    }

    fun extendAllInPlace(decls: List<Decl>)
        = decls.forEach(::extendInPlace)

    fun reduceInPlace(decl: Decl) {
        val nEnv = decl.reduce(this)

        _elements = nEnv.elements
        _refs = nEnv.refs
        _projections = nEnv.projections
        _expressionCache = nEnv.expressionCache
    }

    fun reduce(decl: Decl) : Env
        = decl.reduce(this)

    fun reduceAll(decls: List<Decl>) : Env
        = decls.fold(this) { acc, next -> acc.reduce(next) }

    fun <R> manage(decl: Decl, block: (Env) -> R) : R {
        extendInPlace(decl)
        val result = block(this)
        reduceInPlace(decl)

        return result
    }

    fun getArrows(name: String) : List<AnyArrow> {
        val arrows = mutableListOf<AnyArrow>()
        for (alias in elements.filterIsInstance<IType.Alias>()) {
            if (alias.name == name && alias.type is AnyArrow) arrows.add(alias.type)
        }

        return arrows
    }

    fun getSignatures(name: String) : List<IType.Signature> {
        val signatures = mutableListOf<IType.Signature>()
        for (signature in elements.filterIsInstance<IType.Signature>()) {
            if (signature.name == name) signatures.add(signature)
        }

        return signatures
    }

    fun getSignatures(name: String, receiverType: AnyType) : List<IType.Signature> {
        val signatures = mutableListOf<IType.Signature>()
        for (signature in elements.filterIsInstance<IType.Signature>()) {
            if (signature.name == name && TypeUtils.checkEq(this, receiverType, signature.receiver)) signatures.add(signature)
        }

        return signatures
    }

    fun withSelf(type: AnyType) : Env
        = extend(Decl.TypeAlias("Self", type))

    fun <R> withSelf(type: AnyType, block: (Env) -> R) : R
        = manage(Decl.TypeAlias("Self", type), block)

    fun getSelfType() : AnyType
        = elements.filterIsInstance<IType.Alias>().first { it.name == "Self" }.type

    fun withMatch(type: AnyType): Env {
        val decl = Decl.TypeAlias("__match", type, Decl.ConflictStrategy.Replace)

        return reduce(decl).extend(decl)
    }

    fun withProjectedTrait(trait: IType.Trait) : Env
        = extend(Decl.TypeAlias("__projectedTrait", trait))

    fun getProjectedTrait() : IType.Trait
        = elements.filterIsInstance<IType.Alias>().first { it.name == "__projectedTrait" }.type as IType.Trait

    fun withProjectedType(type: AnyType) : Env
        = extend(Decl.TypeAlias("__projectedType", type))

    fun getProjectedType() : AnyType
        = elements.filterIsInstance<IType.Alias>().first { it.name == "__projectedType" }.type

    fun withProjectedSignature(name: String) : Env? {
        val trait = getProjectedTrait()
        val signature = trait.signatures.firstOrNull { it.name == name }
            ?: return null

        return extend(Decl.Assignment("__projectedSignature", signature))
    }

    fun getProjectedSignature() : IType.Signature?
        = refs.firstOrNull { it.name == "__projectedSignature" }?.type as? IType.Signature

    fun getMatchType() : AnyType
        = elements.filterIsInstance<IType.Alias>().first { it.name == "__match" }.type

    fun denyElement(id: String): Env {
        val nElements = elements.map {
            when (it.id) {
                id -> IType.Never("$id is not defined in the current Context", id)
                else -> it
            }
        }

        return Env(name, nElements, refs, projections, expressionCache, context)
    }

    fun denyRef(name: String): Env {
        val nRefs = refs.map {
            when (it.name) {
                name -> Ref(name, IType.Never("$name is not bound in the current Context", name))
                else -> it
            }
        }

        return Env(name, elements, nRefs, projections, expressionCache, context)
    }

    inline fun <reified O: IType.IOperatorArrow<*, *>> getOperators() : List<O>
        = elements.filterIsInstance<IType.Alias>()
            .map { it.type }
            .filterIsInstance<O>()

    fun import(module: IOrbModule) : Env
        = extend(Decl.Merge(module.getPublicAPI()))

    operator fun plus(other: Env) : Env
        = other.extend(Decl.Merge(this))

    override fun prettyPrint(depth: Int) : String {
        val indent = "\t".repeat(depth)
        val allTypes = elements.joinToString("\n$indent") { it.prettyPrint(depth + 1) }
        val allRefs = refs.joinToString("\n$indent") { it.prettyPrint(depth + 1) }
        val printer = getKoinInstance<Printer>()
        val prettyName = printer.apply(components.joinToString(" & "), PrintableKey.Bold, PrintableKey.Italics)

        return "$indent$prettyName\n$indent$allTypes\n$indent$allRefs"
    }

    override fun exists(env: Env): AnyType = this

    override fun toString(): String = when (elements.isEmpty()) {
        true -> "{}"
        else -> prettyPrint()
    }
}

fun Env.withName(newName: String) : Env = Env(newName, elements, refs, projections, expressionCache, context)

fun Env.withElements(newElements: List<AnyType>) : Env = Env(name, elements + newElements, refs, projections, expressionCache, context)
fun Env.withElementsReplaced(newElements: List<AnyType>) : Env = Env(name, newElements, refs, projections, expressionCache, context)
fun Env.withElement(newElement: AnyType) : Env = withElements(listOf(newElement))
fun Env.withoutElement(element: AnyType) : Env = Env(name, elements - element, refs, projections, expressionCache, context)
fun Env.withoutElements(predicate: (AnyType) -> Boolean) : Env = withElements(elements.filterNot(predicate))
fun Env.withoutElements(elems: List<AnyType>) : Env = Env(name, elements - elems.toSet(), refs, projections, expressionCache, context)

fun Env.withRefs(newRefs: List<IRef>) : Env = Env(name, elements, refs + newRefs, projections, expressionCache, context)
fun Env.withRef(newRef: IRef) : Env = withRefs(listOf(newRef))
fun Env.withAlias(name: String, ref: IRef) : Env = withRef(Alias(name, ref))
fun Env.withoutRef(ref: IRef) : Env = Env(name, elements, refs - ref, projections, expressionCache, context)
fun Env.withoutAlias(name: String) : Env = Env(name, elements, refs.filterNot { it.name == name }, projections, expressionCache, context)

fun Env.withProjections(newProjections: List<Projection>) : Env = Env(name, elements, refs, projections + newProjections, expressionCache, context)
fun Env.withProjection(newProjection: Projection) : Env = withProjections(listOf(newProjection))
fun Env.withProjection(type: AnyType, trait: IType.Trait) : Env = withProjection(Projection(type, trait))
fun Env.withoutProjection(type: AnyType, trait: IType.Trait) : Env = Env(name, elements, refs, projections - Projection(type, trait), expressionCache, context)

fun Env.withContext(newContext: Context) : Env = Env(name, elements, refs, projections, expressionCache, context + newContext)