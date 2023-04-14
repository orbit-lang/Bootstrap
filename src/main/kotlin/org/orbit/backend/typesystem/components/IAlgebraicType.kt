package org.orbit.backend.typesystem.components

interface IAlgebraicType<Self : IAlgebraicType<Self>> : AnyType, IConstructableType<Self>