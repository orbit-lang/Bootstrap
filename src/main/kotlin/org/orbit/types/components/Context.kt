package org.orbit.types.components

import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.components.CompilationEvent
import org.orbit.core.components.CompilationEventBusAware
import org.orbit.core.components.CompilationEventBusAwareImpl
import org.orbit.serial.Serial
import org.orbit.serial.Serialiser
import org.orbit.util.Invocation
import java.io.Serializable
import java.lang.NullPointerException

data class MissingTypeException(val typeName: String) : Exception("Missing type: $typeName")

class Context(builtIns: Set<TypeProtocol> = IntrinsicTypes.allTypes + IntOperators.all()) : Serial, Serializable, CompilationEventBusAware by CompilationEventBusAwareImpl {
    sealed class Events(override val identifier: String) : CompilationEvent {
        class TypeCreated(type: TypeProtocol) : Events("(Context) Type Added: ${type.name}")
        class TypeProjectionCreated(typeProjection: TypeProjection) : Events("(Context) Type Projection Added: ${typeProjection.type.name} -> ${typeProjection.trait.name}")
        class BindingCreated(name: String, type: TypeProtocol) : Events("(Context) Binding Created: $name -> ${type.name}")
    }

    constructor(builtIns: List<TypeProtocol>) : this(builtIns.toSet())
    constructor(vararg builtIns: TypeProtocol) : this(builtIns.toSet())
    internal constructor(vararg builtIns: String) : this(builtIns.map { Type(it, isRequired = false) })

    constructor(other: Context) : this() {
        this.types.addAll(other.types)
        this.bindings.putAll(other.bindings)
        this.typeProjections.addAll(other.typeProjections)
    }

    val types: MutableSet<TypeProtocol> = builtIns.toMutableSet()
    val bindings = mutableMapOf<String, TypeProtocol>()
    private val typeProjections = mutableListOf<TypeProjection>()

    private var next = 0

    init {
        types.addAll(builtIns)
    }

    fun bind(name: String, type: TypeProtocol) {
        bindings[name] = type
        next += 1

        compilationEventBus.notify(Events.BindingCreated(name, type))
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
        return typeProjections.firstOrNull { it.type == type && it.trait == trait }
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

    fun refresh(type: TypeProtocol) : TypeProtocol {
        return types.find { it.name == type.name }!!
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

    fun getTypeOrNull(name: String) : TypeProtocol? {
        val matches = types.filter { it.name == name }

        return when (matches.size) {
            0 -> null
            1 -> when (val type = matches.first()) {
                is TypeAlias -> type.targetType
//                is TypeConstructor -> {
//
//                }
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

    override fun describe(json: JSONObject) {
        val typesJson = types.map { Serialiser.serialise(it) }

        json.put("context.types", typesJson)
    }
}