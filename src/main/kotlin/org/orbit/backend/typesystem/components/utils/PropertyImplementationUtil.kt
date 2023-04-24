package org.orbit.backend.typesystem.components.utils

import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.inference.TraitMemberVerificationResult
import org.orbit.backend.typesystem.utils.TypeUtils

data class PropertyImplementationUtil(private val property: Property) : IImplementationUtil {
    private fun checkProperties(a: Property, b: Property, env: ITypeEnvironment) : Boolean
        = a.name == b.name && TypeUtils.checkEq(env, a.type, b.type)

    override fun isImplemented(by: AnyType, env: ITypeEnvironment): TraitMemberVerificationResult = when (val flat = by.flatten(by, env)) {
        is IStructuralType -> {
            val matches = flat.getProperties().filter { checkProperties(it, property, env) }

            when (matches.count()) {
                1 -> TraitMemberVerificationResult.Implemented(matches[0])
                0 -> TraitMemberVerificationResult.NotImplemented("Property $property is not defined for Type $by")
                else -> {
                    val pretty = matches.joinToString(", ")
                    TraitMemberVerificationResult.NotImplemented("Property $property is defined multiple times for Type $by:\n\t$pretty")
                }
            }
        }

        else -> TraitMemberVerificationResult.NotImplemented("Property $property is not defined for non-Structural Type $by")
    }
}