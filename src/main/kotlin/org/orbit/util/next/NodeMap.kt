package org.orbit.util.next

import com.google.gson.*
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.Node
import org.orbit.core.phase.Phase
import org.orbit.types.next.components.*
import org.orbit.types.next.inference.ITypeRef
import org.orbit.util.Invocation
import org.orbit.util.Printer

interface ITypeMapInterface

interface ITypeMapRead : ITypeMapInterface {
    fun find(name: String) : TypeComponent?
    fun findShallow(name: String) : TypeComponent?
    fun get(node: Node) : TypeComponent?
    fun getConformance(type: TypeComponent) : List<ITrait>
    fun toCtx() : Ctx
    fun getTypeErrors() : List<Never>
    fun filter(fn: (TypeComponent) -> Boolean) : List<TypeComponent>
    fun getContexts(type: TypeComponent) : List<ContextInstantiation>
    fun getExtensions(type: TypeComponent) : List<Extension>
}

interface ITypeMapWrite : ITypeMapInterface {
    fun declare(type: DeclType)
    fun set(node: Node, value: TypeComponent, mergeOnCollision: Boolean = false)
    fun addConformance(type: TypeComponent, trait: ITrait)
    fun addExtension(type: TypeComponent, extension: Extension)
    fun addContext(type: TypeComponent, context: ContextInstantiation)
}

interface ITypeMap : ITypeMapRead, ITypeMapWrite

fun ITypeMapRead.find(path: Path) : TypeComponent?
    = find(path.toString(OrbitMangler))

inline fun <reified P: Phase<*, *>> ITypeMapRead.find(path: Path, invocation: Invocation, node: Node) : TypeComponent {
    val printer = Printer(invocation.platform.getPrintableFactory())

    return find(path)
        ?: Never("Unknown Type ${path.toString(printer)}", node.firstToken.position)
}

interface IAlias : DeclType {
    val target: TypeComponent
}

class TypeMap constructor(): ITypeMap {
    constructor(other: TypeMap) : this() {
        this.map.putAll(other.map)
        this.contextMap.putAll(other.contextMap)
        this.visibleTypes.putAll(other.visibleTypes)
        this.conformanceMap.putAll(other.conformanceMap)
        this.extensionMap.putAll(other.extensionMap)
    }

    private val map = mutableMapOf<String, String>()
    private val contextMap = mutableMapOf<String, List<ContextInstantiation>>()
    private val visibleTypes = mutableMapOf<String, TypeComponent>()
    private val conformanceMap = mutableMapOf<String, List<String>>()
    private val extensionMap = mutableMapOf<String, List<String>>()

    override fun declare(type: DeclType) {
        visibleTypes[type.fullyQualifiedName] = type
    }

    override fun toCtx(): Ctx = Ctx().apply {
        visibleTypes.values.map(::extend)
        conformanceMap.forEach {
            val type = findAs<TypeComponent>(it.key) ?: return@forEach
            val traits = it.value.mapNotNull { s -> findAs<ITrait>(s) }

            traits.forEach { tr -> map(type, tr) }
        }

        contextMap.forEach { (t, u) -> this.map(t, u) }
    }

    override fun filter(fn: (TypeComponent) -> Boolean): List<TypeComponent>
        = visibleTypes.values.filter(fn)

    override fun getTypeErrors(): List<Never>
        = visibleTypes.values.filterIsInstance<Never>()

    override fun addConformance(type: TypeComponent, trait: ITrait) {
        val conformance = conformanceMap[type.fullyQualifiedName]
            ?: emptyList()

        conformanceMap[type.fullyQualifiedName] = conformance + trait.fullyQualifiedName
    }

    override fun addExtension(type: TypeComponent, extension: Extension) {
        val extensions = extensionMap[type.fullyQualifiedName]
            ?: emptyList()

        extensionMap[type.fullyQualifiedName] = extensions + extension.fullyQualifiedName
    }

    override fun getConformance(type: TypeComponent): List<ITrait> {
        val conformance = conformanceMap[type.fullyQualifiedName]
            ?: return emptyList()

        return conformance.mapNotNull(::findAs)
    }

    override fun getExtensions(type: TypeComponent): List<Extension> {
        val extensions = extensionMap[type.fullyQualifiedName]
            ?: return emptyList()

        return extensions.mapNotNull(::findAs)
    }

    fun <T: TypeComponent> findAs(name: String) : T? = when (val type = visibleTypes[name]) {
        is Alias -> type.target as? T
        is PolymorphicType<*> -> type.baseType as? T
        else -> type as? T
    }

    override fun find(name: String): TypeComponent? = when (val type = visibleTypes[name]) {
        // Aliases can be > 1 level deep, so we recurse through until we find the root Type
        is Alias -> find(type.target.fullyQualifiedName)
        else -> type
    }

    override fun findShallow(name: String): TypeComponent?
        = visibleTypes[name]

    fun find(path: Path) : TypeComponent?
        = find(OrbitMangler.mangle(path))

    override fun set(node: Node, value: TypeComponent, mergeOnCollision: Boolean) {
        if (value is DeclType && value !is ITypeRef) declare(value)

        map[node.id] = value.inferenceKey()
    }

    override fun addContext(type: TypeComponent, context: ContextInstantiation) {
        val key = when (type) {
            is Extension -> type.extends.fullyQualifiedName
            else -> type.fullyQualifiedName
        }

        val contexts = contextMap[key] ?: emptyList()

        contextMap[key] = contexts + context
    }

    override fun get(node: Node): TypeComponent? {
        val key = map[node.id] ?: return null

        return find(key)
    }

    override fun getContexts(type: TypeComponent): List<ContextInstantiation>
        = contextMap[type.fullyQualifiedName] ?: emptyList()
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