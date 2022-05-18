package org.orbit.types.next.components

import org.orbit.core.components.SourcePosition
import org.orbit.types.next.intrinsics.Native
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

// If a Type only declares fields of constant Type, it too is constant
// (i.e. its instances have a valid/complete compile-time representation)
fun IType.permitsConstantValues() : Boolean
    = getFields().all { it.type is IType && it.type.permitsConstantValues() }

//fun IType.getDynamicFields() : List<Field>

data class IntConstantValue(override val value: Int) : IConstantValue<Int> {
    override val type: TypeComponent = Native.Types.Int.type
}

data class BoolConstantValue(override val value: Boolean) : IConstantValue<Boolean> {
    override val type: TypeComponent = Native.Types.Bool.type
}

data class SymbolConstantValue(override val value: String) : IConstantValue<String> {
    override val type: TypeComponent = Native.Types.Symbol.type
}

data class InstanceConstantValue(override val type: IType, override val value: List<Field>) : IConstantValue<List<Field>>, FieldAwareType {
    override val fullyQualifiedName: String get() {
        val pretty = value.joinToString(", ") { "${it.name} : ${it.type.fullyQualifiedName}" }

        return "({$pretty} : ${type.fullyQualifiedName})"
    }

    override fun getFields(): List<Field> = value
}

data class TypeConstantValue(override val value: TypeComponent) : IConstantValue<TypeComponent> {
    override val type: TypeComponent = Native.Types.Mirror(value).type

    override val fullyQualifiedName: String = "{${value.fullyQualifiedName} : ${type.fullyQualifiedName}}"
}