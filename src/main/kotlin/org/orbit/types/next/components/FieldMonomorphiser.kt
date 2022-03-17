package org.orbit.types.next.components

object FieldMonomorphiser : Monomorphiser<Field, IType, Field> {
    override fun monomorphise(ctx: Ctx, input: Field, over: IType): MonomorphisationResult<Field> {
        return MonomorphisationResult.Total(Field(input.fullyQualifiedName, over))
    }
}