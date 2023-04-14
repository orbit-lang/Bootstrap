package org.orbit.backend.typesystem.components

interface IConstructor<T : AnyType> : IArrow<IConstructor<T>> {
    val constructedType: T
}