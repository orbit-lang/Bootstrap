package org.orbit.types.next.components

object NominalEq : ITypeEq<TypeComponent, TypeComponent> {
    override fun eq(ctx: Ctx, a: TypeComponent, b: TypeComponent): Boolean = when (b) {
        is Anything -> true
        else -> a.fullyQualifiedName == b.fullyQualifiedName
    }
}