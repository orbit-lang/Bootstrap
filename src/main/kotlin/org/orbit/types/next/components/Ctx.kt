package org.orbit.types.next.components

import org.orbit.types.next.inference.TypeReference
import org.orbit.types.next.intrinsics.Native

interface IContextRead {
    fun getTypes() : List<TypeComponent>
    fun getConformanceMap() : Map<TypeComponent, List<ITrait>>
    fun getType(name: String) : TypeComponent?
    fun getContexts(type: TypeComponent) : List<ContextInstantiation>
    fun <T: TypeComponent> getTypeAs(name: String) : T?
}

interface IContextWrite {
    fun extend(type: TypeComponent)
    fun map(key: TypeComponent, value: ITrait)
    fun map(type: String, contexts: List<ContextInstantiation>)
}

interface IContext : IContextRead, IContextWrite

class Ctx constructor() : IContext {
    private val types = mutableListOf<TypeComponent>()
    private val conformanceMap = mutableMapOf<TypeComponent, List<ITrait>>()
    private val contextMap = mutableMapOf<String, List<ContextInstantiation>>()

    private constructor(types: List<TypeComponent>, conformanceMap: Map<TypeComponent, List<ITrait>>) : this() {
        this.types.addAll(types)
        this.conformanceMap.putAll(conformanceMap)
    }

    override fun getTypes() : List<TypeComponent> = types
    override fun getConformanceMap() : Map<TypeComponent, List<ITrait>> = conformanceMap
    override fun getContexts(type: TypeComponent): List<ContextInstantiation>
        = contextMap[type.fullyQualifiedName] ?: emptyList()

    override fun <T : TypeComponent> getTypeAs(name: String): T?
        = getType(name) as? T

    override fun getType(name: String): TypeComponent? {
        if (name == "_") return Type.hole

        return types.find { it.fullyQualifiedName == name }
    }

    fun deref(ref: TypeComponent) : TypeComponent = when (ref) {
        is TypeReference -> types.find { it.fullyQualifiedName == ref.fullyQualifiedName } ?: ref
        else -> ref
    }

    fun <A: TypeComponent, B: TypeComponent, R> deref(ref1: TypeComponent, ref2: TypeComponent, block: (A, B) -> R) : R {
        val nA = deref(ref1) as A
        val nB = deref(ref2) as B

        return block(nA, nB)
    }

    fun <T: TypeComponent, R> dereference(ref: T, block: (T) -> R) : R {
        val type = types.find { it.fullyQualifiedName == ref.fullyQualifiedName } ?: return block(ref)

        return block(type as T)
    }

    fun <R> dereference(a: TypeComponent, b: TypeComponent, block: (TypeComponent, TypeComponent) -> R) : R {
        val nA = deref(a)
        val nB = deref(b)

        return block(nA, nB)
    }

    fun merge(other: Ctx) : Ctx {
        val distinctTypes = (types + other.types).distinctBy { it.fullyQualifiedName }

        val distinctConformanceKeys = (conformanceMap.keys + other.conformanceMap.keys)
            .distinctBy { it.fullyQualifiedName }

        val distinctConformance = mutableMapOf<TypeComponent, List<ITrait>>()
        for (key in distinctConformanceKeys) {
            val values1 = getConformance(key)
            val values2 = other.getConformance(key)
            val merged = (values1 + values2).distinctBy { it.fullyQualifiedName }

            distinctConformance[key] = merged
        }

        return Ctx(distinctTypes, distinctConformance)
    }

    fun getSignatures(type: Type) : List<ISignature> {
        val baseSignatures = types.filterIsInstance<Signature>().filter {
            AnyEq.weakEq(this, it.getReceiverType(), type)
        }

        val extendedSignatures = types.filterIsInstance<Extension>().flatMap { it.signatures }
        val projectedSignatures = types.filterIsInstance<Projection>().flatMap {
            val m = it.projectedProperties.map { p -> p.project() }

            m.filterIsInstance<Signature>()
        }

        return baseSignatures + extendedSignatures + projectedSignatures
    }

    fun getConformance(type: TypeComponent) : List<ITrait>
        = (conformanceMap.filter { it.key == type }
            .values.firstOrNull() ?: emptyList()).map { entry -> when (entry) {
                is MonomorphicType<*> -> entry.specialisedType as ITrait
                else -> entry
            }}

    override fun extend(type: TypeComponent) {
        if (types.none { it.fullyQualifiedName == type.fullyQualifiedName }) {
            types.add(type)
        }
    }

    override fun map(key: TypeComponent, value: ITrait) = when (val conf = conformanceMap[key]) {
        null -> conformanceMap[key] = listOf(value)
        else -> conformanceMap[key] = (conf + value).distinctBy { it.fullyQualifiedName }
    }

    override fun map(type: String, contexts: List<ContextInstantiation>) {
        contextMap[type] = contexts
    }
}