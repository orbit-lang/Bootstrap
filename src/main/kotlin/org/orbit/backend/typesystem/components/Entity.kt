package org.orbit.backend.typesystem.components

sealed interface Entity<E : Entity<E>> : IType