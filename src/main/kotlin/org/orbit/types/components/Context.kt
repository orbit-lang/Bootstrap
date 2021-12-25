package org.orbit.types.components

import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.components.CompilationEvent
import org.orbit.core.components.CompilationEventBusAware
import org.orbit.core.components.CompilationEventBusAwareImpl
import org.orbit.core.getPath
import org.orbit.core.nodes.Node
import org.orbit.serial.Serial
import org.orbit.serial.Serialiser
import org.orbit.util.ImportManager
import org.orbit.util.Invocation
import java.io.Serializable
import java.lang.NullPointerException

data class MissingTypeException(val typeName: String) : Exception("Missing type: $typeName")

interface ContextProtocol {
    val universe: List<TypeProtocol>
    val monomorphisedTypes: Map<String, Type>

    fun registerMonomorphisation(type: Type) {}
}

fun ContextProtocol.refresh(type: TypeProtocol) : TypeProtocol {
    return universe.find { it.name == type.name }!!
}

class Context(builtIns: Set<TypeProtocol> = IntrinsicTypes.allTypes + IntOperators.all()) : Serial, Serializable, CompilationEventBusAware by CompilationEventBusAwareImpl, ContextProtocol {
    companion object : KoinComponent {
        private val importManager: ImportManager by inject()
    }

    sealed class Events(override val identifier: String) : CompilationEvent {
        class TypeCreated(type: TypeProtocol) : Events("(Context) Type Added: ${type.name}")
        class TypeProjectionCreated(typeProjection: TypeProjection) : Events("(Context) Type Projection Added: ${typeProjection.type.name} -> ${typeProjection.trait.name}")
        class BindingCreated(name: String, type: TypeProtocol) : Events("(Context) Binding Created: $name -> ${type.name}")
    }

    constructor(builtIns: List<TypeProtocol>) : this(builtIns.toSet())
    constructor(vararg builtIns: TypeProtocol) : this(builtIns.toSet())
    internal constructor(vararg builtIns: String) : this(builtIns.map { Type(it, isRequired = false) })

    constructor(other: Context) : this() {
        merge(other.types.toList())
        //this.types.addAll(other.types)
        this.bindings.putAll(other.bindings)
        this.typeProjections.addAll(other.typeProjections)
        this.monomorphisedTypes = other.monomorphisedTypes
    }

    val types: MutableSet<TypeProtocol> = builtIns.toMutableSet()
    val bindings = mutableMapOf<String, TypeProtocol>()

    override val universe: List<TypeProtocol>
        get() = types.toList()

    private val typeProjections = mutableListOf<TypeProjection>()
    override var monomorphisedTypes = mutableMapOf<String, Type>()
        private set

    private var next = 0

    init {
        types.addAll(builtIns)

        val importedTypes = importManager.allTypes

        for (t in importedTypes) {
            if (types.none { it.name == t.name }) {
                types.add(t)
            }
        }
    }

    fun <T> withSubContext(block: (Context) -> T) : T = block(Context(this))

    override fun registerMonomorphisation(type: Type) {
        // HERE - Checking name is not enough for type constructors!

        val previouslyMonomorphised = monomorphisedTypes.filter {
            it.key.startsWith(type.name)
        }

        if (previouslyMonomorphised.isNotEmpty()) {
            var path = OrbitMangler.unmangle(type.name)
            for (p in type.properties) {
                path += OrbitMangler.unmangle(p.type.name)
            }

            val nType = Type(path, type.typeParameters, type.properties, type.traitConformance, type.equalitySemantics, type.isRequired, type.isEphemeral, type.typeConstructor)

            monomorphisedTypes[path.toString(OrbitMangler)] = nType
        } else if (!monomorphisedTypes.containsKey(type.name)) {
            monomorphisedTypes[type.name] = type
        }
    }

    fun bind(name: String, type: TypeProtocol) {
        bindings[name] = type
        next += 1

        compilationEventBus.notify(Events.BindingCreated(name, type))
    }

    private fun merge(other: List<TypeProtocol>) {
        for (type in other) {
            if (types.any { it.name == type.name }) {
                replace(type)
            } else {
                types.add(type)
            }
        }
    }

    fun add(typeProjection: TypeProjection) {
        typeProjections.add(typeProjection)
        compilationEventBus.notify(Events.TypeProjectionCreated(typeProjection))
    }

    fun add(type: TypeProtocol) {
        types.removeIf { it::class.java == type::class.java && it.name == type.name }
        types.add(type)
        compilationEventBus.notify(Events.TypeCreated(type))
    }

    fun addAll(types: List<TypeProtocol>) = types.forEach(::add)

    fun getTypeProjectionOrNull(type: Type, trait: Trait) : TypeProjection? {
        return typeProjections.firstOrNull { it.type.name == type.name && it.trait.name == trait.name }
    }

    fun getTypeProjection(type: Type, trait: Trait) : TypeProjection
        = getTypeProjectionOrNull(type, trait)!!

    fun get(name: String) : TypeProtocol? {
        val type = bindings[name]

        return when (type is TypeAlias) {
            true -> type.targetType
            else -> type
        }
    }

    inline fun <reified T> refreshOrNull(type: TypeProtocol) : T? {
        return types.find { it.name == type.name } as? T
    }

    fun getType(name: String) : TypeProtocol {
        try {
            return getTypeOrNull(name)!!
        } catch (_: NullPointerException) {
            throw MissingTypeException(name)
        }
    }

    fun getTypeByPath(path: Path) : TypeProtocol = getType(path.toString(OrbitMangler))
    fun getTypeOrNull(path: Path) : TypeProtocol? = getTypeOrNull(path.toString(OrbitMangler))
    fun getType(node: Node) = getTypeByPath(node.getPath())

    fun getTypeOrNull(name: String) : TypeProtocol? {
        val matches = types.filter { it.name == name }

        return when (matches.size) {
            0 -> null
            1 -> when (val type = matches.first()) {
                is TypeAlias -> type.targetType
                else -> type
            }
            else -> throw RuntimeException("TODO - Multiple types named '$name'")
        }
    }

    fun remove(name: String) {
        bindings.remove(name)
    }

    fun removeAll(names: List<String>) {
        names.forEach { remove(it) }
    }

    fun replace(type: TypeProtocol) {
        remove(type.name)
        add(type)
    }

    fun replaceMonomorphisedType(type: Type) {
        monomorphisedTypes[type.name] = type
    }

    override fun describe(json: JSONObject) {
        val typesJson = types.map { Serialiser.serialise(it) }

        json.put("context.types", typesJson)
    }
}