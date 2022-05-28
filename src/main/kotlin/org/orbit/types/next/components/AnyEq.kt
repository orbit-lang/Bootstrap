package org.orbit.types.next.components

import org.orbit.types.next.intrinsics.Native

object MonoEq : ITypeEq<MonomorphicType<*>, TypeComponent> {
    override fun eq(ctx: Ctx, a: MonomorphicType<*>, b: TypeComponent): Boolean = when (b) {
        is MonomorphicType<*> -> {
            val polyEq = AnyEq.eq(ctx, a.polymorphicType, b.polymorphicType)
            val paramsEq = a.concreteParameters.count() == b.concreteParameters.count()
                && a.concreteParameters.zip(b.concreteParameters).all { AnyEq.eq(ctx, it.first, it.second) }

            polyEq && paramsEq
        }
        is PolymorphicType<*> -> PolyEq.eq(ctx, b, a)
        else -> when (val mono = TypeMonomorphiser.getPolymorphicSource(b)) {
            null -> AnyEq.eq(ctx, a.specialisedType, b)
            else -> AnyEq.eq(ctx, a, mono)
        }
    }
}

object PolyEq : ITypeEq<PolymorphicType<*>, TypeComponent> {
    override fun eq(ctx: Ctx, a: PolymorphicType<*>, b: TypeComponent): Boolean = when (a.baseType) {
        is MonomorphicType<*> -> when (b) {
            is MonomorphicType<*> -> {
                val baseEq = eq(ctx, a.baseType.polymorphicType, b.polymorphicType)
                // TODO - Assumes we're always checking 0th->nth Type Parameters
                //  Monomorphic Types need to track the positions of their concrete params
                val params = a.baseType.concreteParameters.zip(b.concreteParameters)

                baseEq && params.all { AnyEq.eq(ctx, it.first, it.second) }
            }

            else -> false
        }

        else -> when (b) {
            is PolymorphicType<*> -> AnyEq.eq(ctx, a.baseType, b.baseType)
            is Type -> TypeEq.eq(ctx, b, a)
            else -> false
        }
    }
}

object FamilyEq : ITypeEq<TypeFamily<*>, TypeComponent> {
    override fun eq(ctx: Ctx, a: TypeFamily<*>, b: TypeComponent): Boolean = when (b) {
        is TypeFamily<*> -> NominalEq.eq(ctx, a, b)
        is Anything -> true
        else -> when (a.compare(ctx, b)) {
            is TypeRelation.Member<*> -> true
            else -> false
        }
    }
}

object TypeEq : ITypeEq<Type, TypeComponent> {
    override fun eq(ctx: Ctx, a: Type, b: TypeComponent): Boolean = when (b) {
        is Type -> NominalEq.eq(ctx, a, b)
        is MonomorphicType<*> -> when (val mono = TypeMonomorphiser.getPolymorphicSource(a)) {
            null -> false
            else -> AnyEq.eq(ctx, mono, b)
        }
        is PolymorphicType<*> -> when (a.getMembers().count() == b.partialFields.count()) {
            true -> {
                val zip = b.partialFields.zip(a.getMembers()).map { Pair(it.first.type as AbstractTypeParameter, it.second.type) }

                zip.all { AnyEq.eq(ctx, it.first, it.second) }
            }
            else -> false
        }
        else -> false
    }
}

object TraitEq : ITypeEq<Trait, TypeComponent> {
    fun weakEq(ctx: Ctx, a: Trait, b: TypeComponent): Boolean = when (b) {
        // TODO - Vomit!
        is Kind -> NominalEq.eq(ctx, a, Native.Traits.Kind.trait)
        is Type, is ITypeParameter -> StructuralEq.weakEq(ctx, a, b)
        else -> NominalEq.eq(ctx, a, b)
    }

    override fun eq(ctx: Ctx, a: Trait, b: TypeComponent): Boolean = when (b) {
        // TODO - Vomit!
        is Kind -> NominalEq.eq(ctx, a, Native.Traits.Kind.trait)
        is Type, is ITypeParameter -> StructuralEq.eq(ctx, a, b)
        else -> NominalEq.eq(ctx, a, b)
    }
}

object ParameterEq : ITypeEq<ITypeParameter, TypeComponent> {
    override fun eq(ctx: Ctx, a: ITypeParameter, b: TypeComponent): Boolean = when (a.compare(ctx, b)) {
        is TypeRelation.Same -> true
        else -> false
    }
}

object KindEq : ITypeEq<Kind, TypeComponent> {
    override fun eq(ctx: Ctx, a: Kind, b: TypeComponent): Boolean = when (b) {
        is Kind -> NominalEq.eq(ctx, a, b)
        else -> false
    }
}

object ValueEq : ITypeEq<IConstantValue<*>, TypeComponent> {
    override fun eq(ctx: Ctx, a: IConstantValue<*>, b: TypeComponent): Boolean = when (b) {
        is IConstantValue<*> -> AnyEq.eq(ctx, a.type, b.type) && a.value == b.value
        else -> false
    }
}

object AnyEq : ITypeEq<TypeComponent, TypeComponent> {
    fun weakEq(ctx: Ctx, a: TypeComponent, b: TypeComponent): Boolean = ctx.dereference(a, b) { a, b ->
        when (a) {
            is Infer -> true
            is Anything -> true
            is IConstantValue<*> -> ValueEq.eq(ctx, a, b)
            is Kind -> KindEq.eq(ctx, a, b)
            is MonomorphicType<*> -> MonoEq.eq(ctx, a, b)
            is PolymorphicType<*> -> PolyEq.eq(ctx, a, b)
            is Type -> TypeEq.eq(ctx, a, b)
            is Trait -> TraitEq.weakEq(ctx, a, b)
            is TypeFamily<*> -> FamilyEq.eq(ctx, a, b)
            is ITypeParameter -> ParameterEq.eq(ctx, a, b)
            else -> NominalEq.eq(ctx, a, b)
        }
    }

    override fun eq(ctx: Ctx, a: TypeComponent, b: TypeComponent): Boolean {
        val a = ctx.deref(a)
        val b = ctx.deref(b)

        return when (a) {
            is Infer -> true
            is Anything -> true
            is IConstantValue<*> -> ValueEq.eq(ctx, a, b)
            is Kind -> KindEq.eq(ctx, a, b)
            is MonomorphicType<*> -> MonoEq.eq(ctx, a, b)
            is PolymorphicType<*> -> PolyEq.eq(ctx, a, b)
            is Type -> TypeEq.eq(ctx, a, b)
            is Trait -> TraitEq.eq(ctx, a, b)
            is TypeFamily<*> -> FamilyEq.eq(ctx, a, b)
            is ITypeParameter -> ParameterEq.eq(ctx, a, b)
            else -> NominalEq.eq(ctx, a, b)
        }
    }
}