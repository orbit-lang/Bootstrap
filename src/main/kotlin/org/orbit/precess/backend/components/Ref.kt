package org.orbit.precess.backend.components

import org.orbit.precess.backend.utils.AnyEntity
import org.orbit.precess.backend.utils.AnyType

interface IRef {
    val name: String
    val type: AnyType
    val uniqueId: String

    fun getHistory() : List<RefEntry>
    fun consume() : Ref
}

inline fun <reified E : RefEntry> IRef.getHistoryInstances(): List<E> = getHistory().filterIsInstance<E>()

class Ref(override val name: String, override val type: AnyType) : IRef {
    private val history = mutableListOf<RefEntry>()
    override val uniqueId: String = "$name:${type.id}"

    override fun getHistory(): List<RefEntry> = history

    override fun consume(): Ref = apply {
        history.add(RefEntry.Use(this))
    }

    override fun toString(): String = "$name:${type.id}"
}

data class Alias(override val name: String, val ref: IRef) : IRef {
    override val type: AnyType = ref.type
    override val uniqueId: String = "$name:${ref.type.id}"

    override fun getHistory(): List<RefEntry> = ref.getHistory()
    override fun consume(): Ref = ref.consume()

    override fun toString(): String = "$name:($ref)"
}