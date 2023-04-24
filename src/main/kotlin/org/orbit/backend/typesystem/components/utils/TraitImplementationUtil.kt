package org.orbit.backend.typesystem.components.utils

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.components.Trait
import org.orbit.backend.typesystem.inference.TraitMemberVerificationResult

data class TraitImplementationUtil(private val trait: Trait) : IImplementationUtil {
    override fun isImplemented(by: AnyType, env: ITypeEnvironment): TraitMemberVerificationResult = when (val flat = by.flatten(by, env)) {
        is Trait -> TraitMemberVerificationResult.NotImplemented("Trait $flat cannot implement Trait $trait")
        else -> {
            val propertyResults = trait.properties.fold(TraitMemberVerificationResult.None as TraitMemberVerificationResult) { acc, next ->
                val util = PropertyImplementationUtil(next)

                acc + util.isImplemented(by, env)
            }

            val signatureResult = trait.signatures.fold(TraitMemberVerificationResult.None as TraitMemberVerificationResult) { acc, next ->
                val util = SignatureImplementationUtil(next)

                acc + util.isImplemented(by, env)
            }

            propertyResults + signatureResult
        }
    }
}
