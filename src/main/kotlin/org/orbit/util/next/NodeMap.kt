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
    fun find(name: String) : TypeComponent?
    fun get(node: Node) : TypeComponent?
    fun getConformance(type: Type) : List<ITrait>
    fun toCtx() : Ctx
    fun getTypeErrors() : List<Never>
}

interface ITypeMapWrite : ITypeMapInterface {
    fun declare(type: DeclType)
    fun set(node: Node, value: TypeComponent, mergeOnCollision: Boolean = false)
    fun addConformance(type: Type, trait: ITrait)
}

interface ITypeMap : ITypeMapRead, ITypeMapWrite

fun ITypeMapRead.find(path: Path) : TypeComponent?
    = find(path.toString(OrbitMangler))

inline fun <reified P: Phase<*, *>> ITypeMapRead.find(path: Path, invocation: Invocation, node: Node) : TypeComponent {
    val printer = Printer(invocation.platform.getPrintableFactory())

    return find(path)
        ?: Never("Unknown Type ${path.toString(printer)}", node.firstToken.position)
}

interface IAlias : TypeComponent {
    val target: TypeComponent
}

data class Alias(override val fullyQualifiedName: String, override val target: TypeComponent) : IAlias {
    override val isSynthetic: Boolean = true

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation
        = target.compare(ctx, other)
}

class TypeMap : ITypeMap {
    private val map = mutableMapOf<String, String>()
    private val visibleTypes = mutableMapOf<String, TypeComponent>()
    private val conformanceMap = mutableMapOf<String, List<String>>()

    override fun declare(type: DeclType) {
        visibleTypes[type.fullyQualifiedName] = type
    }

    override fun toCtx(): Ctx = Ctx().apply {
        visibleTypes.values.map(::extend)
        conformanceMap.forEach {
            val type = findAs<Type>(it.key) ?: return@forEach
            val traits = it.value.mapNotNull { s -> findAs<ITrait>(s) }

            traits.forEach { tr -> map(type, tr) }
        }
    }

    override fun getTypeErrors(): List<Never>
        = visibleTypes.values.filterIsInstance<Never>()

    override fun addConformance(type: Type, trait: ITrait) {
        val conformance = conformanceMap[type.fullyQualifiedName]
            ?: emptyList()

        conformanceMap[type.fullyQualifiedName] = conformance + trait.fullyQualifiedName
    }

    override fun getConformance(type: Type): List<Trait> {
        val conformance = conformanceMap[type.fullyQualifiedName]
            ?: return emptyList()

        return conformance.mapNotNull(::findAs)
    }

    fun <T: TypeComponent> findAs(name: String) : T? = when (val type = visibleTypes[name]) {
        is Alias -> type.target as? T
        else -> type as? T
    }

    override fun find(name: String): TypeComponent? = when (val type = visibleTypes[name]) {
        is Alias -> type.target
        else -> type
    }

    fun find(path: Path) : TypeComponent?
        = find(OrbitMangler.mangle(path))

    override fun set(node: Node, value: TypeComponent, mergeOnCollision: Boolean) {
        if (!mergeOnCollision && map.containsKey(node.id))
            throw RuntimeException("FATAL - Node ID Collision: ${node.id}")

        if (value is DeclType) declare(value)

        map[node.id] = value.inferenceKey()
    }

    override fun get(node: Node): TypeComponent? {
        val key = map[node.id] ?: return null

        return find(key)
    }
}

interface IBindingScope {
    fun bind(name: String, type: TypeComponent)
    fun getType(name: String) : TypeComponent?
}

sealed class BindingScope : IBindingScope {
    object Root : BindingScope()
    class Leaf(val parent: IBindingScope) : BindingScope()

    private val bindings = mutableMapOf<String, TypeComponent>()

    override fun bind(name: String, type: TypeComponent) {
        bindings[name] = type
    }

    override fun getType(name: String) : TypeComponent? = bindings[name]
}

fun IBindingScope.getTypeOrNever(name: String) : TypeComponent = when (val type = getType(name)) {
    null -> Never("Could not infer type of $name")
    else -> type
}