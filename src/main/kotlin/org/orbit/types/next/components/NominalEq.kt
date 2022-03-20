package org.orbit.types.next.components

object NominalEq : ITypeEq<TypeComponent, TypeComponent> {
    override fun eq(ctx: Ctx, a: TypeComponent, b: TypeComponent): Boolean
        = a.fullyQualifiedName == b.fullyQualifiedName
}