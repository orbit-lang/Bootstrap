package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.components.Enum
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.core.nodes.EnumCaseReferenceNode
import org.orbit.util.Invocation

object EnumCaseReferenceInference : ITypeInference<EnumCaseReferenceNode, ITypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: EnumCaseReferenceNode, env: ITypeEnvironment): AnyType {
        val caseName = node.case.identifier
        val matches = env.mapTypes<Enum, EnumCase> { it.getCaseOrNull(caseName) }

        if (matches.isEmpty()) {
            throw invocation.make<TypeSystem>("Cannot resolve Type of Enum Case named `$caseName`", node)
        } else if (matches.count() > 1) {
            val pretty = matches.joinToString("\n\t")

            throw invocation.make<TypeSystem>("Multiple matches found for Enum Case `$caseName`:\n\t$pretty\nDisambiguate by qualifying the Case name with its Enum Type, e.g. `MyEnum.$caseName`", node)
        }

        return matches[0]
    }
}