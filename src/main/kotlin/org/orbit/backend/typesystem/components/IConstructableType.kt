package org.orbit.backend.typesystem.components

import org.orbit.backend.typesystem.utils.TypeUtils

sealed interface IConstructableType<Self: IConstructableType<Self>> : ISpecialisedType {
    fun getConstructor(given: List<AnyType>) : IConstructor<Self>? {
        val constructors = getConstructors()

        return constructors.firstOrNull {
            if (it.getDomain().count() != given.count()) return@firstOrNull false

            it.getDomain().zip(given).all { p -> TypeUtils.checkEq(GlobalEnvironment, p.first, p.second) }
        } as? IConstructor<Self>
    }
}