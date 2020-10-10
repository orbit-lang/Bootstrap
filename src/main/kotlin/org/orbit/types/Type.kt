package org.orbit.types

interface Type {
    val name: String
    val members: List<Member>
    val behaviours: List<Behaviour>
}

operator fun Type.plus(name: String) : Member {
    return Member(name, this)
}