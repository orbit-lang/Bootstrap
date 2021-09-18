package org.orbit.types.components

import org.json.JSONObject
import org.orbit.serial.Serial
import org.orbit.util.AnyPrintable
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import java.io.Serializable

interface TypeProtocol : Serial, Serializable, AnyPrintable {
    val name: String
    val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol>

    fun isSatisfied(context: Context, type: TypeProtocol) : Boolean {
        return (equalitySemantics as AnyEquality).isSatisfied(context, this, type)
    }

    override fun describe(json: JSONObject) {
        json.put("type.meta", javaClass.simpleName)
        json.put("type.name", name)
    }

    override fun toString(printer: Printer): String
        = printer.apply(name, PrintableKey.Bold, PrintableKey.Italics)
}

typealias TypeProtocolPair = Pair<TypeProtocol, TypeProtocol>

fun TypeProtocolPair.isSatisfied(context: Context) : Boolean {
    return first.isSatisfied(context, second)
}

interface ValuePositionType : TypeProtocol {
    val isEphemeral: Boolean
}

operator fun Entity.plus(name: String) : Property {
    return Property(name, this)
}