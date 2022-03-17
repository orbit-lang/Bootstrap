package org.orbit.types.next.components

import org.orbit.core.Mangler
import org.orbit.core.Path

fun IType.getPath(mangler: Mangler) : Path
    = mangler.unmangle(fullyQualifiedName)

fun List<IType>.anyEq(ctx: Ctx, target: IType) : Boolean
    = any { AnyEq.eq(ctx, target, it) }

fun List<IType>.filterEq(ctx: Ctx, target: IType) : List<IType>
    = filter { AnyEq.eq(ctx, target, it) }

fun List<Pair<IType, IType>>.allEq(ctx: Ctx) : Boolean
    = all { AnyEq.eq(ctx, it.first, it.second) }

operator fun Int.plus(type: IType) : Pair<Int, IType>
    = Pair(this, type)