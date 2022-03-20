package org.orbit.types.next.components

import org.orbit.types.next.inference.TypeReference

interface IContextRead {
    fun getTypes() : List<Type>
    fun getTraits() : List<Trait>
    fun getSignatureMap() : Map<Type, List<Signature>>
    fun getConformanceMap() : Map<TypeComponent, List<Trait>>

    fun getType(name: String) : Type?
    fun getTrait(name: String) : Trait?
}

interface IContextWrite {
    fun extend(type: TypeComponent)
    fun map(key: Type, value: Signature)
    fun map(key: TypeComponent, value: Trait)
}

interface IContext : IContextRead, IContextWrite

class Ctx constructor() : IContext {
    private val types = mutableListOf<Type>()
    private val traits = mutableListOf<Trait>()
    private val signatureMap = mutableMapOf<Type, List<Signature>>()
    private val conformanceMap = mutableMapOf<TypeComponent, List<Trait>>()

    private constructor(types: List<Type>, traits: List<Trait>, signatureMap: Map<Type, List<Signature>>, conformanceMap: Map<TypeComponent, List<Trait>>) : this() {
        this.types.addAll(types)
        this.traits.addAll(traits)
        this.signatureMap.putAll(signatureMap)
        this.conformanceMap.putAll(conformanceMap)
    }

    override fun getTypes() : List<Type> = types
    override fun getTraits() : List<Trait> = traits
    override fun getSignatureMap() : Map<Type, List<Signature>> = signatureMap
    override fun getConformanceMap() : Map<TypeComponent, List<Trait>> = conformanceMap

    override fun getType(name: String): Type? {
        return types.find { it.fullyQualifiedName == name }
    }

    override fun getTrait(name: String): Trait? {
        return traits.find { it.fullyQualifiedName == name }
    }

    fun <R> dereferencing(ref: TypeComponent, block: (TypeComponent) -> R) : R = when (ref) {
        is TypeReference -> {
            val type = types.find { it.fullyQualifiedName == ref.fullyQualifiedName }
                ?: traits.find { it.fullyQualifiedName == ref.fullyQualifiedName }!!

            block(type)
        }

        else -> block(ref)
    }

    fun merge(other: Ctx) : Ctx {
        val distinctTypes = (types + other.types).distinctBy { it.fullyQualifiedName }
        val distinctTraits = (traits + other.traits).distinctBy { it.fullyQualifiedName }

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

        return Ctx(distinctTypes, distinctTraits, distinctSignatures, distinctConformance)
    }

    fun getSignatures(type: Type) : List<Signature>
        = signatureMap.filter { it.key == type }
            .values.firstOrNull() ?: emptyList()

    fun getConformance(type: TypeComponent) : List<Trait>
        = conformanceMap.filter { it.key == type }
            .values.firstOrNull() ?: emptyList()

    private fun extend(type: Type) {
        if (types.none { it.fullyQualifiedName == type.fullyQualifiedName }) {
            types.add(type)
        }
    }

    private fun extend(trait: Trait) {
        if (traits.none { it.fullyQualifiedName == trait.fullyQualifiedName }) {
            traits.add(trait)
        }
    }

    override fun extend(type: TypeComponent) = when (type) {
        is Type -> extend(type)
        is Trait -> extend(type)
        else -> TODO("???")
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