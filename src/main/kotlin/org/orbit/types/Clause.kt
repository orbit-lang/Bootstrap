package org.orbit.types

interface Clause {
    fun satisfied() : Boolean
}

interface Equality : Clause {
    val source: Type
    val target: Type
}

data class NominalEquality(override val source: Type, override val target: Type) : Equality {
    override fun satisfied(): Boolean {
        return source.name == target.name
    }
}

data class StructuralEquality(override val source: Type, override val target: Type) : Equality {
    override fun satisfied(): Boolean {
        val memberships = source.members.map { Membership(target, it) }
        val behaviours = source.behaviours.map { Implements(target, it) }
        val contract = Contract(memberships + behaviours)

        return contract.satisfied()
    }
}

data class Membership(private val type: Type, private val member: Member) : Clause {
    override fun satisfied(): Boolean {
        val sourceMember = type.members.find {
            StructuralEquality(member.type, it.type).satisfied()
                && it.name == member.name
        } ?: return false

        return StructuralEquality(sourceMember.type, type)
            .satisfied()
    }
}

data class Implements(private val type: Type, private val behaviour: Behaviour) : Clause {
    override fun satisfied(): Boolean {
        return true
    }
}



