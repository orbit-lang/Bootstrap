package org.orbit.graph

import org.orbit.core.OrbitMangler
import org.orbit.core.Path

data class Binding(val kind: Binding.Kind, val simpleName: String, val path: Path) {
	interface Kind {
		interface Container : Kind
		interface Entity : Kind
		object Api : Container
		object Module : Container
		object Type : Entity
		object Trait : Entity
		object Self : Entity
		object Ephemeral : Entity
		object Method : Kind
	}

//	enum class Kind {
//		Api, Module, Type, Trait, Package, Method, Self;
//
//		override fun toString() : String
//			= this.name
//	}

	override fun equals(other: Any?) : Boolean = when (other) {
		// Bindings can share a simple name, but must resolve to unique canonical names
		is Binding -> other.kind == kind && other.simpleName == simpleName && other.path == path
		else -> false
	}

	override fun toString() : String = "$kind: ${path.toString(OrbitMangler)}"
}