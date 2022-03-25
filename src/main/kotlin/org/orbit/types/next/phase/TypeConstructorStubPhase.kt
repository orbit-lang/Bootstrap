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

object TypeConstructorStubPhase : EntityConstructorStubPhase<TypeConstructorNode, Type> {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<TypeConstructorNode>): PolymorphicType<Type> {
        val parameters = input.inferenceUtil.inferAllAs<TypeIdentifierNode, Parameter>(input.node.typeParameterNodes, TypeLiteralInferenceContext.TypeParameterContext)

        parameters.forEach { input.inferenceUtil.declare(it) }

        val fields = input.inferenceUtil.inferAllAs<PairNode, Field>(input.node.properties,
            AnyInferenceContext(PairNode::class.java)
        )

        val baseType = Type(input.node.getPath(), fields)

        return PolymorphicType(baseType, parameters)
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
                else -> Parameter(parameter.fullyQualifiedName, constraints)
            }
        }

        return PolymorphicType(typeConstructor.baseType, nParameters)
    }
}