package org.orbit.types.next.phase

import org.koin.core.component.inject
import org.orbit.core.getPath
import org.orbit.core.nodes.FamilyConstructorNode
import org.orbit.core.nodes.TypeConstructorNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.types.next.components.AbstractTypeParameter
import org.orbit.types.next.components.PolymorphicType
import org.orbit.types.next.components.TypeFamily
import org.orbit.types.next.inference.TypeLiteralInferenceContext
import org.orbit.util.Invocation

object FamilyConstructorStubPhase : EntityConstructorStubPhase<FamilyConstructorNode, TypeFamily<*>> {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<FamilyConstructorNode>): PolymorphicType<TypeFamily<*>> {
        val parameters = input.inferenceUtil.inferAllAs<TypeIdentifierNode, AbstractTypeParameter>(input.node.typeParameterNodes,
            TypeLiteralInferenceContext.TypeParameterContext
        )

        parameters.forEach { input.inferenceUtil.declare(it) }

        val members = input.node.entities.map {
            TypeConstructorStubPhase.execute(TypePhaseData(input.inferenceUtil, it as TypeConstructorNode))
        }

        val baseFamily = TypeFamily(input.node.getPath(), members)

        // TODO - Trait conformance on Type Families
        return PolymorphicType(baseFamily, parameters, partialFields = emptyList(), traitConformance = emptyList())
    }
}