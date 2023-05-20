package org.orbit.backend.typesystem.utils

import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatformTools
import org.orbit.backend.typesystem.components.*

sealed interface ITypeUnifier<A: AnyType, B: AnyType> {
    fun unify(env: ITypeEnvironment, a: A, b: B) : AnyType
}

object TypeTypeUnifier : ITypeUnifier<Type, Type> {
    override fun unify(env: ITypeEnvironment, a: Type, b: Type): AnyType = when (TypeUtils.checkEq(env, a, b)) {
        true -> a
        else -> Always
    }
}

object ArrowArrowUnifier : ITypeUnifier<ArrowBox, ArrowBox> {
    override fun unify(env: ITypeEnvironment, a: ArrowBox, b: ArrowBox): AnyType {
        val aArrow = a.arrow
        val bArrow = b.arrow

        val aDomain = aArrow.getDomain().map { it.erase() }
        val aCodomain = aArrow.getCodomain().erase()
        val xArrow = aDomain.arrowOf(aCodomain)

        val bDomain = bArrow.getDomain().map { it.erase() }
        val bCodomain = bArrow.getCodomain().erase()
        val yArrow = bDomain.arrowOf(bCodomain)

        if (aDomain.count() != bDomain.count()) return Always

        if (TypeUtils.checkEq(env, xArrow, yArrow)) {
            return xArrow
        }

        return Always
    }
}

object TypeUnificationUtil {
    fun unify(env: ITypeEnvironment, a: AnyType, b: AnyType) : AnyType {
        val aErased = a.erase()
        val bErased = b.erase()
        val aClazz = aErased::class.java.simpleName
        val bClazz = bErased::class.java.simpleName

        val inference = KoinPlatformTools.defaultContext().get().get<ITypeUnifier<AnyType, AnyType>>(named("unify${aClazz}_$bClazz"))

        return inference.unify(env, aErased, bErased)
    }
}