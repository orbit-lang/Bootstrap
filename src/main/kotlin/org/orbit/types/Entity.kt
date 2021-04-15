package org.orbit.types

import org.orbit.core.OrbitMangler
import org.orbit.core.Path

data class Entity(
    override val name: String,
    override val members: List<Member> = emptyList(),
    override val behaviours: List<Behaviour> = emptyList()) : Type {
    constructor(path: Path, members: List<Member> = emptyList()) : this(path.toString(OrbitMangler), members)
}

enum class IntrinsicTypes(val type: Type) {
    Unit(Entity("Orb::Types::Intrinsics::Unit")),
    Int(Entity("Orb::Types::Intrinsics::Int")),
    Symbol(Entity("Orb::Types::Intrinsics::Symbol"));

    companion object {
        val allTypes: Set<Type>
            get() = values().map { it.type }.toSet()
    }
}