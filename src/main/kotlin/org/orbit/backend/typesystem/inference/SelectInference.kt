package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.CaseNode
import org.orbit.core.nodes.ElseNode
import org.orbit.core.nodes.SelectNode
import org.orbit.util.Invocation

object SelectInference : ITypeInference<SelectNode, AnnotatedSelfTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: SelectNode, env: AnnotatedSelfTypeEnvironment): AnyType {
        val typeAnnotation = env.typeAnnotation
        val conditionType = TypeInferenceUtils.infer(node.condition, env)
        val nEnv = CaseTypeEnvironment(env, env.getSelfType(), conditionType)
        val caseTypes = TypeInferenceUtils.inferAllAs<CaseNode, IType.Case>(node.cases, nEnv)
        val cardinality = caseTypes.fold(ITypeCardinality.Zero as ITypeCardinality) { acc, next -> acc + next.getCardinality() }
        val hasElseCase = node.cases.any { it.pattern is ElseNode }

        if (cardinality is ITypeCardinality.Infinite) {
            return when (hasElseCase) {
                true -> typeAnnotation
                else -> throw invocation.make<TypeSystem>("Type `$conditionType` has an infinite number of cases and therefore requires an Else case", node)
            }
        }

        val constructableType = conditionType.flatten(nEnv) as? IType.ICaseIterable<*>
            ?: throw invocation.compilerError<TypeSystem>("Cannot perform Case analysis on non-constructable Type `$conditionType`", node.condition)

        // If this is not an Infinite Type, ensure all possible cases are covered
        val actualCases = caseTypes.map { it.condition }
        val expectedCases = constructableType.getCases(typeAnnotation).map { it.condition }

        val coveredCases = actualCases.map { it.id }.distinct()
        val conditionCardinality = constructableType.getCardinality()

        if (!hasElseCase && conditionCardinality is ITypeCardinality.Finite && actualCases.count() != conditionCardinality.count) {
            val missingCases = expectedCases.filterNot { it.id in coveredCases }
            val prettyMissing = missingCases.joinToString("\n\t")

            throw invocation.make<TypeSystem>("Missing ${missingCases.count()}/${expectedCases.count()} Case(s) for Select expression of Type `$constructableType`:\n\t$prettyMissing", node)
        }

        return typeAnnotation
    }
}