package org.orbit.backend.typesystem.components.utils

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.components.TraitMember
import org.orbit.backend.typesystem.inference.TraitMemberVerificationResult

sealed interface IImplementationUtil {
    fun isImplemented(by: AnyType, env: ITypeEnvironment) : TraitMemberVerificationResult
}

