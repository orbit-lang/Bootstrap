package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.getPath
import org.orbit.core.nodes.*
import org.orbit.types.next.components.*
import org.orbit.types.next.inference.AnyInferenceContext
import org.orbit.types.next.inference.TypeConstraint
import org.orbit.types.next.inference.TypeLiteralInferenceContext
import org.orbit.util.Invocation
import org.orbit.util.Printer

object TypeConstructorStubPhase : EntityConstructorStubPhase<TypeConstructorNode, Type> {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<TypeConstructorNode>): PolymorphicType<Type> {
        val parameters = input.inferenceUtil.inferAllAs<TypeIdentifierNode, AbstractTypeParameter>(input.node.typeParameterNodes, TypeLiteralInferenceContext.TypeParameterContext)

        parameters.forEach { input.inferenceUtil.declare(it) }

        val fields = input.inferenceUtil.inferAllAs<PairNode, Field>(input.node.properties, AnyInferenceContext(PairNode::class.java))

        val baseType = Type(input.node.getPath(), fields)

        return PolymorphicType(baseType, parameters, partialFields = emptyList(), traitConformance = emptyList())
    }
}

object TypeConstructorConformancePhase : TypePhase<TypeConstructorNode, PolymorphicType<IType>>, KoinComponent {
    override val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun run(input: TypePhaseData<TypeConstructorNode>): PolymorphicType<IType> {
        val typeConstructor = input.inferenceUtil.inferAs<TypeIdentifierNode, PolymorphicType<IType>>(input.node.typeIdentifierNode)
        val traitConformance = input.inferenceUtil.inferAll(input.node.traitConformance, AnyInferenceContext(TypeExpressionNode::class.java))

        for (tc in traitConformance) {
            if (tc !is Trait) {
                if (tc is MonomorphicType<*> && tc.specialisedType !is Trait) {
                    throw Never("Type Constructor ${typeConstructor.toString(printer)} cannot conform to non-Trait (${tc.kind.toString(printer)}) ${tc.toString(printer)}", input.node.firstToken.position)
                }
            }
        }

        return PolymorphicType(typeConstructor.baseType, typeConstructor.parameters, traitConformance as List<ITrait>, typeConstructor.isSynthetic, typeConstructor.partialFields)
    }
}

object TypeConstructorConstraintsPhase : TypePhase<TypeConstructorNode, PolymorphicType<IType>>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<TypeConstructorNode>): PolymorphicType<IType> {
        val typeConstructor = input.inferenceUtil.inferAs<TypeIdentifierNode, PolymorphicType<Type>>(input.node.typeIdentifierNode)

        val wheres = input.inferenceUtil.inferAllAs<TypeConstraintWhereClauseNode, TypeConstraint>(input.node.clauses, AnyInferenceContext(TypeConstraintWhereClauseNode::class.java))

        val nParameters = typeConstructor.parameters.map { parameter ->
            val constraints = wheres.filter { it.source == parameter }

            when (constraints.isEmpty()) {
                true -> parameter
                else -> AbstractTypeParameter(parameter.fullyQualifiedName, constraints)
            }
        }

        return PolymorphicType(typeConstructor.baseType, nParameters, typeConstructor.traitConformance, typeConstructor.isSynthetic, typeConstructor.partialFields)
    }
}