package org.orbit.types

data class Entity(
    override val name: String,
    override val members: List<Member> = emptyList(),
    override val behaviours: List<Behaviour> = emptyList()) : Type