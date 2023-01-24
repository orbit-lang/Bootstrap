package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.IMutableTypeEnvironment
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.StructuralPatternEnvironment
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.*
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance
import kotlin.math.exp

object IdentifierBindingPatternInference : ITypeInference<IdentifierBindingPatternNode, StructuralPatternEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: IdentifierBindingPatternNode, env: StructuralPatternEnvironment): AnyType {
        // TODO - This is completely wrong! We need to check by index here, not name
        val member = env.structuralType.members.firstOrNull { it.first == node.identifier.identifier }
            ?: throw invocation.make<TypeSystem>("Cannot pattern match on Structural Type ${env.structuralType} because it does not declare a Member called `${node.identifier.identifier}`", node.identifier)

        return IType.PatternBinding(member)
    }
}

object TypedIdentifierBindingPatternInference : ITypeInference<TypedIdentifierBindingPatternNode, StructuralPatternEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: TypedIdentifierBindingPatternNode, env: StructuralPatternEnvironment): AnyType {
        val member = env.structuralType.members.firstOrNull { it.first == node.identifier.identifier }
            ?: throw invocation.make<TypeSystem>("Cannot pattern match on Structural Type ${env.structuralType} because it does not declare a Member called `${node.identifier.identifier}`", node.identifier)

        val providedType = TypeInferenceUtils.infer(node.typePattern, env)

        if (!TypeUtils.checkEq(env, providedType, member.second)) {
            throw invocation.make<TypeSystem>("Cannot pattern match on Structural Type ${env.structuralType} because of mismatched types: expected ${member.second}, found $providedType", node.typePattern)
        }

        return IType.PatternBinding(member)
    }
}

object DiscardBindingPatternInference : ITypeInference<DiscardBindingPatternNode, StructuralPatternEnvironment> {
    override fun infer(node: DiscardBindingPatternNode, env: StructuralPatternEnvironment): AnyType
        = IType.PatternBinding("_", IType.Always)
}

object StructuralPatternInference : ITypeInference<StructuralPatternNode, IMutableTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: StructuralPatternNode, env: IMutableTypeEnvironment): AnyType {
        val patternType = TypeInferenceUtils.infer(node.typeExpressionNode, env)
        val struct = patternType.flatten(patternType, env) as? IType.IStructuralType
            ?: throw invocation.make<TypeSystem>("Cannot pattern match on non-Structural Type $patternType in Case expression", node)

        val nEnv = StructuralPatternEnvironment(env, struct)
        val bindingTypes = TypeInferenceUtils.inferAllAs<IBindingPatternNode, IType.PatternBinding>(node.bindings, nEnv)

        val expected = struct.members.count()
        val provided = bindingTypes.count()

        if (provided < expected) {
            val allProvided = bindingTypes.map { it.name }
            val missing = struct.members.filterNot { it.first in allProvided }
            val printer = getKoinInstance<Printer>()
            val pretty = missing.joinToString("\n\t") {
                val name = printer.apply(it.first, PrintableKey.Italics)

                "$name: ${it.second}"
            }

            throw invocation.make<TypeSystem>("Cannot pattern match on Structural Type $patternType because the following members are not bound:\n\t$pretty", node)
        } else if (provided > expected) {
            throw invocation.make<TypeSystem>("Cannot pattern match on Structural Type $patternType because too many bindings have been provided: expected $expected, found $provided", node)
        }

        bindingTypes.forEach { env.bind(it.name, it.type, it.index) }

        return struct
    }
}