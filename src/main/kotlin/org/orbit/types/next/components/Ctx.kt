package org.orbit.types.next.components

import org.orbit.types.next.inference.TypeReference

interface IContextRead {
    fun getTypes() : List<TypeComponent>
    fun getSignatureMap() : Map<Type, List<Signature>>
    fun getConformanceMap() : Map<TypeComponent, List<Trait>>

    fun getType(name: String) : TypeComponent?
    fun <T: TypeComponent> getTypeAs(name: String) : T?
}

interface IContextWrite {
    fun extend(type: TypeComponent)
    fun map(key: Type, value: Signature)
    fun map(key: TypeComponent, value: Trait)
}

interface IContext : IContextRead, IContextWrite

class Ctx constructor() : IContext {
    private val types = mutableListOf<TypeComponent>()
    private val signatureMap = mutableMapOf<Type, List<Signature>>()
    private val conformanceMap = mutableMapOf<TypeComponent, List<Trait>>()

    private constructor(types: List<TypeComponent>, signatureMap: Map<Type, List<Signature>>, conformanceMap: Map<TypeComponent, List<Trait>>) : this() {
        this.types.addAll(types)
        this.signatureMap.putAll(signatureMap)
        this.conformanceMap.putAll(conformanceMap)
    }

    override fun getTypes() : List<TypeComponent> = types
    override fun getSignatureMap() : Map<Type, List<Signature>> = signatureMap
    override fun getConformanceMap() : Map<TypeComponent, List<Trait>> = conformanceMap

    override fun <T : TypeComponent> getTypeAs(name: String): T?
        = getType(name) as? T

    override fun getType(name: String): TypeComponent? {
        return types.find { it.fullyQualifiedName == name }
    }

    fun <R> dereferencing(ref: TypeComponent, block: (TypeComponent) -> R) : R = when (ref) {
        is TypeReference -> {
            val type = types.find { it.fullyQualifiedName == ref.fullyQualifiedName }!!

            block(type)
        }

        else -> block(ref)
    }

    fun merge(other: Ctx) : Ctx {
        val distinctTypes = (types + other.types).distinctBy { it.fullyQualifiedName }

        val distinctSignatureKeys = (signatureMap.keys + other.signatureMap.keys)
            .distinctBy { it.fullyQualifiedName }

        val distinctSignatures = mutableMapOf<Type, List<Signature>>()
        for (key in distinctSignatureKeys) {
            val values1 = getSignatures(key)
            val values2 = other.getSignatures(key)
            val merged = (values1 + values2).distinctBy { it.fullyQualifiedName }

            distinctSignatures[key] = merged
        }

        val distinctConformanceKeys = (conformanceMap.keys + other.conformanceMap.keys)
            .distinctBy { it.fullyQualifiedName }

        val distinctConformance = mutableMapOf<TypeComponent, List<Trait>>()
        for (key in distinctConformanceKeys) {
            val values1 = getConformance(key)
            val values2 = other.getConformance(key)
            val merged = (values1 + values2).distinctBy { it.fullyQualifiedName }

            distinctConformance[key] = merged
        }

        return Ctx(distinctTypes, distinctSignatures, distinctConformance)
    }

    fun getSignatures(type: Type) : List<Signature>
        = signatureMap.filter { it.key == type }
            .values.firstOrNull() ?: emptyList()

    fun getConformance(type: TypeComponent) : List<Trait>
        = conformanceMap.filter { it.key == type }
            .values.firstOrNull() ?: emptyList()

    override fun extend(type: TypeComponent) {
        if (types.none { it.fullyQualifiedName == type.fullyQualifiedName }) {
            types.add(type)
        }
    }

    override fun map(key: Type, value: Signature) = when (val sigs = signatureMap[key]) {
        null -> signatureMap[key] = listOf(value)
        else -> signatureMap[key] = (sigs + value).distinctBy { it.fullyQualifiedName }
    }

    override fun map(key: TypeComponent, value: Trait) = when (val conf = conformanceMap[key]) {
        null -> conformanceMap[key] = listOf(value)
        else -> conformanceMap[key] = (conf + value).distinctBy { it.fullyQualifiedName }
    }


}