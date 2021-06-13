package org.orbit.graph.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import java.io.Serializable

data class Binding(val kind: Kind, val simpleName: String, val path: Path) : Serializable {
	interface Kind : Serializable {
		interface Container : Kind
		interface Entity : Kind
		object Api : Container
		object Module : Container
		object Type : Entity
		object Trait : Entity
		object Self : Entity
		object Ephemeral : Entity
		object Method : Kind

		fun same(other: Kind) : Boolean {
			return this::class.java == other::class.java
		}

		data class Union(val left: Kind, val right: Kind) : Kind {
			companion object {
				val AnyEntity = Union(Kind.Type, Kind.Trait)
			}

			override fun same(other: Kind): Boolean {
				return left.same(other) || right.same(other)
			}
		}
	}

	companion object {
		val Self = Binding(Kind.Self, "Self", Path.self)
	}

	override fun equals(other: Any?) : Boolean = when (other) {
		// Bindings can share a simple name, but must resolve to unique canonical names
		is Binding -> other.kind == kind && other.simpleName == simpleName && other.path == path
		else -> false
	}

	override fun toString() : String = "$kind: ${path.toString(OrbitMangler)}"
}
