package org.orbit.util.next

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.Node
import org.orbit.core.phase.Phase
import org.orbit.types.next.components.*
import org.orbit.util.Invocation
import org.orbit.util.Printer

interface ITypeMapInterface

interface ITypeMapRead : ITypeMapInterface {
    fun find(name: String) : IType?
    fun get(node: Node) : IType?
    fun getConformance(type: Type) : List<Trait>
    fun toCtx() : Ctx
}

interface ITypeMapWrite : ITypeMapInterface {
    fun declare(type: DeclType)
    fun set(node: Node, value: IType, mergeOnCollision: Boolean = false)
    fun addConformance(type: Type, trait: Trait)
}

interface ITypeMap : ITypeMapRead, ITypeMapWrite

fun ITypeMapRead.find(path: Path) : IType?
    = find(path.toString(OrbitMangler))

inline fun <reified P: Phase<*, *>> ITypeMapRead.find(path: Path, invocation: Invocation, node: Node) : IType {
    val printer = Printer(invocation.platform.getPrintableFactory())

    return find(path)
        ?: Never("Unknown Type ${path.toString(printer)}", node.firstToken.position)
}

class TypeMap : ITypeMap {
    private val map = mutableMapOf<String, String>()
    private val visibleTypes = mutableMapOf<String, IType>()
    private val conformanceMap = mutableMapOf<String, List<String>>()

    override fun declare(type: DeclType) {
        visibleTypes[type.fullyQualifiedName] = type
    }

    override fun toCtx(): Ctx = Ctx().apply {
        visibleTypes.values.map(::extend)
        conformanceMap.forEach {
            val type = findAs<Type>(it.key) ?: return@forEach
            val traits = it.value.mapNotNull { s -> findAs<Trait>(s) }

            traits.forEach { tr -> map(type, tr) }
        }
    }

    override fun addConformance(type: Type, trait: Trait) {
        val conformance = conformanceMap[type.fullyQualifiedName]
            ?: emptyList()

        conformanceMap[type.fullyQualifiedName] = conformance + trait.fullyQualifiedName
    }

    override fun getConformance(type: Type): List<Trait> {
        val conformance = conformanceMap[type.fullyQualifiedName]
            ?: return emptyList()

        return conformance.mapNotNull(::findAs)
    }

    fun <T: IType> findAs(name: String) : T?
        = visibleTypes[name] as? T

    override fun find(name: String): IType?
        = visibleTypes[name]

    fun find(path: Path) : IType?
        = find(OrbitMangler.mangle(path))

    override fun set(node: Node, value: IType, mergeOnCollision: Boolean) {
        if (!mergeOnCollision && map.containsKey(node.id))
            throw RuntimeException("FATAL - Node ID Collision: ${node.id}")

        if (value is DeclType) declare(value)

        map[node.id] = value.inferenceKey()
    }

    override fun get(node: Node): IType? {
        val key = map[node.id] ?: return null

        return find(key)
    }
}

interface IBindingScope {
    fun bind(name: String, type: IType)
    fun getType(name: String) : IType?
}

sealed class BindingScope : IBindingScope {
    object Root : BindingScope()
    class Leaf(val parent: IBindingScope) : BindingScope()

    private val bindings = mutableMapOf<String, IType>()

    override fun bind(name: String, type: IType) {
        bindings[name] = type
    }

    override fun getType(name: String) : IType? = bindings[name]
}

fun IBindingScope.getTypeOrNever(name: String) : IType = when (val type = getType(name)) {
    null -> Never("Could not infer type of $name")
    else -> type
}