package org.orbit.types

class Contract(private val clauses: List<Clause> = emptyList()) : Clause {
    private val _clauses = mutableListOf<Clause>()

    init {
        _clauses.addAll(clauses)
    }

    fun extend(clause: Clause) {
        _clauses.add(clause)
    }

    override fun satisfied(): Boolean {
        return clauses.all { it.satisfied() }
    }
}