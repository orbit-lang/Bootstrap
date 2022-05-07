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

object CtxDeserializer : JsonDeserializer<ITypeMapRead> {
    override fun deserialize(json: JsonElement, typeOfT: java.lang.reflect.Type, context: JsonDeserializationContext): ITypeMapRead {
        return TypeMap(json.asJsonObject) as ITypeMapRead
    }
}

interface ITypeMapRead : ITypeMapInterface {
    fun find(name: String) : TypeComponent?
    fun get(node: Node) : TypeComponent?
    fun getConformance(type: TypeComponent) : List<ITrait>
    fun toCtx() : Ctx
    fun getTypeErrors() : List<Never>
    fun filter(fn: (TypeComponent) -> Boolean) : List<TypeComponent>
}

interface ITypeMapWrite : ITypeMapInterface {
    fun declare(type: DeclType)
    fun set(node: Node, value: TypeComponent, mergeOnCollision: Boolean = false)
    fun addConformance(type: TypeComponent, trait: ITrait)
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

class TypeMap constructor() : ITypeMap {
    private val map = mutableMapOf<String, String>()
    private val visibleTypes = mutableMapOf<String, TypeComponent>()
    private val conformanceMap = mutableMapOf<String, List<String>>()

    constructor(json: JsonObject) : this() {
        val m = json.getAsJsonObject("map")

        for (kv in m.entrySet()) {
            map[kv.key] = kv.value.toString()
        }

        val v = json.getAsJsonObject("visibleTypes")

        for (kv in v.entrySet()) {
            visibleTypes[kv.key] = Gson().fromJson(kv.value, TypeComponent::class.java)
        }
    }

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

    override fun getConformance(type: TypeComponent): List<ITrait> {
        val conformance = conformanceMap[type.fullyQualifiedName]
            ?: return emptyList()

        return conformance.mapNotNull(::findAs)
    }

    fun <T: TypeComponent> findAs(name: String) : T? = when (val type = visibleTypes[name]) {
        is Alias -> type.target as? T
        is PolymorphicType<*> -> type.baseType as? T
        else -> type as? T
    }

    override fun find(name: String): TypeComponent? = when (val type = visibleTypes[name]) {
        is Alias -> type.target
        else -> type
    }

    fun find(path: Path) : TypeComponent?
        = find(OrbitMangler.mangle(path))

    override fun set(node: Node, value: TypeComponent, mergeOnCollision: Boolean) {
        if (value is DeclType && value !is ITypeRef) declare(value)

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