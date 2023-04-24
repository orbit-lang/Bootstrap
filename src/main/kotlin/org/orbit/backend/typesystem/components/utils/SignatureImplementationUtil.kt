package org.orbit.backend.typesystem.components.utils

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.components.Signature
import org.orbit.backend.typesystem.components.getSignatures
import org.orbit.backend.typesystem.inference.TraitMemberVerificationResult
import org.orbit.backend.typesystem.utils.TypeUtils

data class SignatureImplementationUtil(private val signature: Signature) : IImplementationUtil {
    private fun checkSignatures(a: Signature, b: Signature, env: ITypeEnvironment) : Boolean {
        if (a.name != b.name) return false

        if (!TypeUtils.checkEq(env, a.receiver, b.receiver)) return false
        if (!TypeUtils.checkEq(env, a.returns, b.returns)) return false

        if (a.parameters.count() != b.parameters.count()) return false

        return a.parameters.zip(b.parameters).all {
            TypeUtils.checkEq(env, it.first, it.second)
        }
    }

    private fun checkImpl(type: AnyType, env: ITypeEnvironment) : TraitMemberVerificationResult {
        var signatures = env.getSignatures().filter {
            TypeUtils.checkEq(env, it.component.receiver, type)
        }

        signatures = signatures.filter {
            checkSignatures(it.component, signature, env)
        }

        return when (signatures.count()) {
            1 -> TraitMemberVerificationResult.Implemented(signatures[0].component)
            0 -> TraitMemberVerificationResult.NotImplemented("Signature $signature is not implemented for Type $type")
            else -> {
                val pretty = signatures.joinToString("\n\t")

                TraitMemberVerificationResult.NotImplemented("Signature $signature is implemented multiple times for Type $type:\n\t$pretty")
            }
        }
    }


    override fun isImplemented(by: AnyType, env: ITypeEnvironment): TraitMemberVerificationResult {
        val flat = by.flatten(by, env)

        return checkImpl(flat, env)
    }
}