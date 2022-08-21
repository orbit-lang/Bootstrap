package org.orbit.precess.backend.components

class Ref(val name: String, val type: IType.Entity<*>) {
    private val history = mutableListOf<RefEntry>()

    val uniqueId: String = "$name:${type.id}"

    fun getHistory(): List<RefEntry> = history
    inline fun <reified E : RefEntry> getHistoryInstances(): List<E> = getHistory().filterIsInstance<E>()

    fun consume(): Ref = apply {
        history.add(RefEntry.Use(this))
    }
}