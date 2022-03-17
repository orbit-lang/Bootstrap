package org.orbit.types.next.components

object NominalEq : ITypeEq<IType, IType> {
    override fun eq(ctx: Ctx, a: IType, b: IType): Boolean
        = a.fullyQualifiedName == b.fullyQualifiedName
}