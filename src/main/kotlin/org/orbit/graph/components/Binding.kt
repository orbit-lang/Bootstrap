package org.orbit.graph.components

import com.google.gson.*
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.orbit.core.GraphEntity
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.util.endsWith
import java.lang.reflect.Type

object KindAdapter : TypeAdapter<Binding.Kind>() {
	override fun write(out: JsonWriter?, value: Binding.Kind?) {
		if (value == null || out == null) return

		out.beginObject()
		if (value is Binding.Kind.Union) {
			out.name("union.left")
			write(out, value.left)
			out.name("union.right")
			write(out, value.right)
		} else {
			out.value(value.getName())
		}
		out.endObject()
	}

	override fun read(`in`: JsonReader?): Binding.Kind {
		if (`in` == null) return Binding.Kind.Empty

		return Binding.Kind.Empty
	}
}

object KindSerialiser : JsonSerializer<Binding.Kind> {
	override fun serialize(src: Binding.Kind, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
		if (src is Binding.Kind.Union) {
			return context.serialize(src.toMap())
		}

		return context.serialize(src.getName())
	}
}

object KindDeserialiser : JsonDeserializer<Binding.Kind> {
	override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Binding.Kind {
		return Binding.Kind.Empty
	}
}

object PathSerialiser : JsonSerializer<Path> {
	override fun serialize(src: Path, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
		return context.serialize(src.toString(OrbitMangler))
	}
}

object PathDeserialiser : JsonDeserializer<Path> {
	override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Path {
		val str = json.asJsonArray

		return Path(str.map { it.asString })
	}
}

data class Binding(val kind: Kind, val simpleName: String, val path: Path, val vertexID: GraphEntity.Vertex.ID? = null) {
	interface Kind {
		interface Container : Kind
		interface Entity : Kind
		interface Projection : Kind
		object Api : Container
		object Module : Container
		object Type : Entity
		object RequiredType : Entity
		object TypeAlias : Entity
		object TypeParameter : Kind
		object TypeConstructor : Kind
		object TraitConstructor : Kind
		object Trait : Entity
		object Self : Entity
		object Ephemeral : Entity
		object Method : Kind
		object TypeProjection : Projection
		object Empty : Kind
		object Context : Kind

		fun getName() : String {
			return javaClass.simpleName
		}

		fun same(other: Kind) : Boolean {
			return this::class.java == other::class.java
		}

		fun contains(kind: Kind) : Boolean {
			return this == kind
		}

		data class Union(val left: Kind, val right: Kind) : Kind {
			companion object {
				val anyEntityConstructor = Union(TypeConstructor, TraitConstructor)
				val container = Union(Module, Api)
				val entity = Union(Union(Union(Type, Trait), TypeAlias), Context)
				val entityOrMethod = Union(entity, Method)
				val receiver = Union(Union(entityOrMethod, anyEntityConstructor), TypeParameter)
				val entityOrConstructor = Union(entity, anyEntityConstructor)
				val entityMethodOrConstructor = Union(entityOrMethod, anyEntityConstructor)
				val entityMethodOrConstructorOrParameter = Union(entityMethodOrConstructor, TypeParameter)
				val entityOrConstructorOrParameter = Union(entityOrConstructor, TypeParameter)
			}

			override fun getName(): String {
				return "${left.getName()} | ${right.getName()}"
			}

			override fun same(other: Kind): Boolean {
				return left.same(other) || right.same(other)
			}

			override fun contains(kind: Kind): Boolean {
				return left.contains(kind) || right.contains(kind)
			}

			fun toMap() : Map<String, Any> {
				val leftValue = when (left) {
					is Union -> left.toMap()
					else -> left
				}

				val rightValue = when (right) {
					is Union -> right.toMap()
					else -> right
				}

				return mapOf("union.left" to leftValue, "union.right" to rightValue)
			}
		}
	}

	companion object {
		val Self = Binding(Kind.Self, "Self", Path.self)
		val empty = Binding(Kind.Empty, "", Path.empty)
		val infer = Binding(Kind.Type, "_", Path.infer)
	}

	fun matches(name: String) : Boolean = when (kind) {
		// If this binding represents a signature, match on the method name rather than last component
		Kind.Method -> name == path.toString(OrbitMangler) || path.first { it.first().isLowerCase() } == name
		else -> name == path.toString(OrbitMangler) || path.endsWith(name)
	}

	override fun equals(other: Any?) : Boolean = when (other) {
		// Bindings can share a simple name, but must resolve to unique canonical names
		is Binding -> other.kind == kind && other.simpleName == simpleName && other.path == path
		else -> false
	}

	override fun toString() : String = "$kind: ${path.toString(OrbitMangler)}"
}
