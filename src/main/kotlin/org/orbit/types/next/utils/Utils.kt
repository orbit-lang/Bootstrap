package org.orbit.types.next.components

import org.orbit.core.Mangler
import org.orbit.core.Path

fun TypeComponent.getPath(mangler: Mangler) : Path
    = mangler.unmangle(fullyQualifiedName)

fun List<TypeComponent>.anyEq(ctx: Ctx, target: TypeComponent) : Boolean
    = any { AnyEq.eq(ctx, target, it) }

fun List<TypeComponent>.filterEq(ctx: Ctx, target: TypeComponent) : List<TypeComponent>
    = filter { AnyEq.eq(ctx, target, it) }

fun List<Pair<TypeComponent, TypeComponent>>.allEq(ctx: Ctx) : Boolean
    = all { AnyEq.eq(ctx, it.first, it.second) }

operator fun Int.plus(type: TypeComponent) : Pair<Int, TypeComponent>
    = Pair(this, type)