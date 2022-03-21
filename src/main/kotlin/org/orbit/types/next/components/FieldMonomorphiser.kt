package org.orbit.types.next.components

object FieldMonomorphiser : Monomorphiser<Field, TypeComponent, Field> {
    override fun monomorphise(ctx: Ctx, input: Field, over: TypeComponent, context: MonomorphisationContext): MonomorphisationResult<Field> {
        return MonomorphisationResult.Total(Field(input.fullyQualifiedName, over))
    }
}