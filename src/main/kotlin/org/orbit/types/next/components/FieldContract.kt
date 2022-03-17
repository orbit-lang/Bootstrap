package org.orbit.types.next.components

data class FieldContract(val field: Field) : Contract {
    override fun isImplemented(ctx: Ctx, by: IType): Boolean {
        if (by !is Type) return false
        if (by.fields.none { it.fullyQualifiedName == field.fullyQualifiedName }) return false

        return by.fields.any { AnyEq.eq(ctx, field.type, it.type) }
    }
}