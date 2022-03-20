package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.getPath
import org.orbit.core.nodes.PairNode
import org.orbit.core.nodes.TraitConstructorNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.types.next.components.*
import org.orbit.types.next.inference.AnyInferenceContext
import org.orbit.types.next.inference.TypeLiteralInferenceContext
import org.orbit.types.next.inference.TypeReference
import org.orbit.util.Invocation

object TraitConstructorStubPhase : EntityConstructorStubPhase<TraitConstructorNode, Trait>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<TraitConstructorNode>): PolymorphicType<Trait> {
        val parameters = input.inferenceUtil.inferAllAs<TypeIdentifierNode, Parameter>(input.node.typeParameterNodes,
            TypeLiteralInferenceContext.TypeParameterContext
        )

        parameters.forEach { input.inferenceUtil.declare(it) }

        val fields = input.inferenceUtil.inferAllAs<PairNode, Field>(input.node.properties,
            AnyInferenceContext(PairNode::class.java)
        )

        val fieldContracts = fields.map { FieldContract(TypeReference(input.node.getPath()), it) }
        val baseType = Trait(input.node.getPath(), fieldContracts)

        return PolymorphicType(baseType, parameters)
    }
}