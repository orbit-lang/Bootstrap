package org.orbit.types.next.components

import org.orbit.types.next.inference.TypeReference

interface IContextRead {
    fun getTypes() : List<TypeComponent>
    fun getConformanceMap() : Map<TypeComponent, List<ITrait>>
    fun getType(name: String) : TypeComponent?
    fun getContext(type: TypeComponent) : ContextInstantiation?
    fun <T: TypeComponent> getTypeAs(name: String) : T?
}

interface IContextWrite {
    fun extend(type: TypeComponent)
    fun map(key: TypeComponent, value: ITrait)
    fun map(type: String, context: ContextInstantiation)
}

interface IContext : IContextRead, IContextWrite

class Ctx constructor() : IContext {
    private val types = mutableListOf<TypeComponent>()
    private val conformanceMap = mutableMapOf<TypeComponent, List<ITrait>>()
    private val contextMap = mutableMapOf<String, ContextInstantiation>()

    private constructor(types: List<TypeComponent>, conformanceMap: Map<TypeComponent, List<ITrait>>) : this() {
        this.types.addAll(types)
        this.conformanceMap.putAll(conformanceMap)
    }

    override fun getTypes() : List<TypeComponent> = types
    override fun getConformanceMap() : Map<TypeComponent, List<ITrait>> = conformanceMap
    override fun getContext(type: TypeComponent): ContextInstantiation?
        = contextMap[type.fullyQualifiedName]

    override fun <T : TypeComponent> getTypeAs(name: String): T?
        = getType(name) as? T

    override fun getType(name: String): TypeComponent? {
        return types.find { it.fullyQualifiedName == name }
    }

    private fun deref(ref: TypeComponent) : TypeComponent = when (ref) {
        is TypeReference -> types.find { it.fullyQualifiedName == ref.fullyQualifiedName }!!
        else -> ref
    }

    fun <R> dereference(ref: TypeComponent, block: (TypeComponent) -> R) : R = when (ref) {
        is TypeReference -> {
            val type = types.find { it.fullyQualifiedName == ref.fullyQualifiedName }!!

            block(type)
        }

        else -> block(ref)
    }

    fun <R> dereference(a: TypeComponent, b: TypeComponent, block: (TypeComponent,TypeComponent) -> R) : R {
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

    fun getSignatures(type: Type) : List<ISignature>
        = types.filterIsInstance<Signature>()
            .filter {
                AnyEq.eq(this, it.getReceiverType(), type)
            }

    fun getConformance(type: TypeComponent) : List<ITrait>
        = conformanceMap.filter { it.key == type }
            .values.firstOrNull() ?: emptyList()

    override fun extend(type: TypeComponent) {
        if (types.none { it.fullyQualifiedName == type.fullyQualifiedName }) {
            types.add(type)
        }
    }

    override fun map(key: TypeComponent, value: ITrait) = when (val conf = conformanceMap[key]) {
        null -> conformanceMap[key] = listOf(value)
        else -> conformanceMap[key] = (conf + value).distinctBy { it.fullyQualifiedName }
    }

    override fun map(type: String, context: ContextInstantiation) {
        contextMap[type] = context
    }
}