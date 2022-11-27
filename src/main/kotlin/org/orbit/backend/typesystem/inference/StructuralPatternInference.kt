package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.IBindingPatternNode
import org.orbit.core.nodes.IdentifierBindingPatternNode
import org.orbit.core.nodes.StructuralPatternNode
import org.orbit.util.Invocation

object IdentifierBindingPatternInference : ITypeInference<IdentifierBindingPatternNode, StructuralPatternEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: IdentifierBindingPatternNode, env: StructuralPatternEnvironment): AnyType {
        val member = env.structuralType.members.firstOrNull { it.first == node.identifier.identifier }
            ?: throw invocation.make<TypeSystem>("Cannot pattern match on Structural Type ${env.structuralType} because it does not declare a Member called `${node.identifier.identifier}`", node.identifier)

        return IType.PatternBinding(member.first, member.second)
    }
}

object StructuralPatternInference : ITypeInference<StructuralPatternNode, IMutableTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: StructuralPatternNode, env: IMutableTypeEnvironment): AnyType {
        val patternType = TypeInferenceUtils.infer(node.typeExpressionNode, env)
        val struct = patternType.flatten(patternType, env) as? IType.Struct
            ?: throw invocation.make<TypeSystem>("Cannot pattern match on non-Structural Type $patternType in Case expression", node)

        val nEnv = StructuralPatternEnvironment(env, struct)
        val bindingTypes = TypeInferenceUtils.inferAllAs<IBindingPatternNode, IType.PatternBinding>(node.bindings, nEnv)

        bindingTypes.forEach { env.bind(it.name, it.type, it.index) }

        return struct
    }
}