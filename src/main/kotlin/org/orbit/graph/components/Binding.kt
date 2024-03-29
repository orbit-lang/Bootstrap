package org.orbit.graph.components

import org.orbit.core.GraphEntity
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.util.endsWith

data class Binding(val kind: Kind, val simpleName: String, val path: Path, val vertexID: GraphEntity.Vertex.ID? = null) {
	interface Kind {
		interface Container : Kind
		interface Entity : Kind

		object Api : Container
		object Module : Container
		object Type : Entity
		object TypeAlias : Entity
		object Trait : Entity
		object Self : Entity
		object Any : Entity
		object Never : Entity
		object Ephemeral : Entity
		object Lambda : Entity
		object Method : Kind
		object Empty : Kind
		object Context : Kind
		object Value : Kind
		object Attribute : Kind

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
				val entity = Union(Union(Union(Union(Union(Type, Trait), Value), TypeAlias), Context), Attribute)
				val entityOrMethod = Union(entity, Method)
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
		val self = Binding(Kind.Self, "Self", Path.self)
		val empty = Binding(Kind.Empty, "", Path.empty)
		val infer = Binding(Kind.Type, "_", Path.infer)
		val array = Binding(Kind.Type, "[]", Path.array)
		val any = Binding(Kind.Any, "Any", Path.any)
		val never = Binding(Kind.Never, "Never", Path.never)
		val lambda = Binding(Kind.Lambda, "->", Path.lambda)
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
