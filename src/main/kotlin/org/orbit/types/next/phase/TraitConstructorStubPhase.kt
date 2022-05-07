package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.getPath
import org.orbit.core.nodes.*
import org.orbit.types.next.components.*
import org.orbit.types.next.inference.*
import org.orbit.util.Invocation

object TraitConstructorStubPhase : EntityConstructorStubPhase<TraitConstructorNode, Trait>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<TraitConstructorNode>): PolymorphicType<Trait> {
        val parameters = input.inferenceUtil.inferAllAs<TypeIdentifierNode, AbstractTypeParameter>(input.node.typeParameterNodes,
            TypeLiteralInferenceContext.TypeParameterContext
        )

        parameters.forEach { input.inferenceUtil.declare(it) }

        val protoTrait = Trait(input.node.getPath())
        val protoPoly = PolymorphicType(protoTrait, parameters, partialFields = emptyList())
        val nInferenceUtil = input.inferenceUtil.derive(retainsTypeMap = true, retainsBindingScope = true, protoPoly)

        val fields = nInferenceUtil.inferAllAs<PairNode, Field>(input.node.properties,
            AnyInferenceContext(PairNode::class.java)
        )

        val fieldContracts = fields.map { FieldContract(TypeReference(input.node.getPath()), it) }
        val baseType = Trait(input.node.getPath(), fieldContracts)

        return PolymorphicType(baseType, parameters, partialFields = emptyList())
    }
}

object TraitConstructorConstraintsPhase : TypePhase<TraitConstructorNode, PolymorphicType<ITrait>>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<TraitConstructorNode>): PolymorphicType<ITrait> {
        val traitConstructor = input.inferenceUtil.inferAs<TypeIdentifierNode, PolymorphicType<ITrait>>(input.node.typeIdentifierNode)

        val wheres = input.inferenceUtil.inferAllAs<TypeConstraintWhereClauseNode, TypeConstraint>(input.node.clauses, AnyInferenceContext(
            TypeConstraintWhereClauseNode::class.java))

        val nParameters = traitConstructor.parameters.map { parameter ->
            val constraints = wheres.filter { it.source == parameter }

            when (constraints.isEmpty()) {
                true -> parameter
                else -> AbstractTypeParameter(parameter.fullyQualifiedName, constraints)
            }
        }

        return PolymorphicType(traitConstructor.baseType, nParameters, partialFields = traitConstructor.partialFields)
    }
}
