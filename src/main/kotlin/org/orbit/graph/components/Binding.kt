package org.orbit.graph.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.util.endsWith
import java.io.Serializable

data class Binding(val kind: Kind, val simpleName: String, val path: Path, val vertexID: GraphEntity.Vertex.ID? = null) : Serializable {
	interface Kind : Serializable {
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
				val anyEntityConstructor = Union(Kind.TypeConstructor, Kind.TraitConstructor)
				val container = Union(Kind.Module, Kind.Api)
				val entity = Union(Union(Kind.Type, Kind.Trait), Kind.TypeAlias)
				val entityOrMethod = Union(entity, Kind.Method)
				val receiver = Union(entityOrMethod,anyEntityConstructor)
				val entityOrConstructor = Union(entity, anyEntityConstructor)
				val entityMethodOrConstructor = Union(entityOrMethod, anyEntityConstructor)
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
		}
	}

	companion object {
		val Self = Binding(Kind.Self, "Self", Path.self)
		val empty = Binding(Kind.Empty, "", Path.empty)
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
