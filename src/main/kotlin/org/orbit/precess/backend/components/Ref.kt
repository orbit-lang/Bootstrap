package org.orbit.precess.backend.components

import org.orbit.precess.backend.utils.AnyType
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

interface IRef {
    val name: String
    val type: AnyType
    val uniqueId: String

    fun getHistory() : List<RefEntry>
    fun consume() : Ref

    fun prettyPrint(depth: Int = 0) : String {
        val indent = "\t".repeat(depth)

        return "$indent$uniqueId"
    }
}

inline fun <reified E : RefEntry> IRef.getHistoryInstances(): List<E> = getHistory().filterIsInstance<E>()

class Ref(override val name: String, override val type: AnyType) : IRef {
    private val history = mutableListOf<RefEntry>()
    override val uniqueId: String = "$name:${type.id}"

    override fun getHistory(): List<RefEntry> = history

    override fun consume(): Ref = apply {
        history.add(RefEntry.Use(this))
    }

    override fun prettyPrint(depth: Int): String {
        val indent = "\t".repeat(depth)
        val printer = getKoinInstance<Printer>()
        val prettyName = printer.apply(name, PrintableKey.Italics)

        return "$indent$type.$prettyName"
    }

    override fun toString(): String = prettyPrint()
}

data class Alias(override val name: String, val ref: IRef) : IRef {
    override val type: AnyType = ref.type
    override val uniqueId: String = "$name:${ref.type.id}"

    override fun getHistory(): List<RefEntry> = ref.getHistory()
    override fun consume(): Ref = ref.consume()

    override fun toString(): String = "$name:($ref)"
}