package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.CaseNode
import org.orbit.core.nodes.ElseNode
import org.orbit.core.nodes.INode
import org.orbit.core.nodes.SelectNode
import org.orbit.util.Invocation

object SelectInference : ITypeInference<SelectNode, AnnotatedSelfTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    private fun inferFiniteType(node: SelectNode, conditionType: AnyType, typeAnnotation: AnyType, env: AnnotatedSelfTypeEnvironment) : AnyType {
        val constructableType = conditionType as? IType.IConstructableType<*>
            ?: throw invocation.compilerError<TypeSystem>("Finite non-constructable type?!", node)

        val nEnv = CaseTypeEnvironment(env, env.getSelfType(), constructableType)
        val nonElseCases = node.cases.filterNot { it.isElseCase }
        val caseConstructors = TypeInferenceUtils.inferAllAs<CaseNode, IType.Case>(nonElseCases, nEnv)
            .map {
                val domain = it.getDomain()[0]
                domain.flatten(domain, env)
                as IType.IConstructor<*>
            }
            .distinctBy { it.getCanonicalName() }
        val cardinality = caseConstructors.fold(ITypeCardinality.Zero as ITypeCardinality) { acc, next -> acc + next.getCardinality() }
        val elseNodes = node.cases.filter { it.isElseCase }

        if (elseNodes.count() > 1) {
            throw invocation.make<TypeSystem>("Multiple Else cases found in Select expression", elseNodes.last())
        }

        val hasElseCase = elseNodes.count() == 1

        when (hasElseCase) {
            true -> {
                val elseType = TypeInferenceUtils.infer(elseNodes[0], nEnv) as IType.Case

                if (!TypeUtils.checkEq(nEnv, elseType.result, typeAnnotation)) {
                    throw invocation.make<TypeSystem>("Else case expected to return $typeAnnotation, found $elseType", elseNodes[0])
                }
            }
            else -> {}
        }

        if (cardinality is ITypeCardinality.Infinite) {
            return when (hasElseCase) {
                true -> typeAnnotation
                else -> throw invocation.make<TypeSystem>("Type `$constructableType` has an infinite number of cases and therefore requires an Else case", node)
            }
        }

        val expectedConstructors = constructableType.getConstructors()
        val conditionCardinality = constructableType.getCardinality()

        if (!hasElseCase && conditionCardinality is ITypeCardinality.Finite && caseConstructors.count() != conditionCardinality.count) {
            val allCaseNames = caseConstructors.map { it.getCanonicalName() }
            val missingCases = expectedConstructors.filterNot { it.getCanonicalName() in allCaseNames }
            val prettyMissing = missingCases.joinToString("\n\t")

            throw invocation.make<TypeSystem>("Missing ${missingCases.count()}/${expectedConstructors.count()} Case(s) for Select expression of Type `$constructableType`:\n\t$prettyMissing", node)
        }

        return typeAnnotation
    }

    private fun inferInfiniteType(node: SelectNode, conditionType: AnyType, typeAnnotation: AnyType, env: AnnotatedSelfTypeEnvironment) : AnyType {
        val elseCases = node.cases.filter { it.isElseCase }
        val nEnv = CaseTypeEnvironment(env, env.getSelfType(), conditionType)

        if (elseCases.count() > 1) {
            throw invocation.make<TypeSystem>("Multiple Else cases found in Select expression", elseCases.last())
        }

        if (elseCases.isEmpty()) {
            throw invocation.make<TypeSystem>("Select expression must contain an Else case where the condition Type (AKA $conditionType) has Infinite Cardinality", node)
        }

        val nonElseCases = node.cases.filterNot { it.isElseCase }
        val expectedCase = IType.Case(conditionType, typeAnnotation)

        for (caseNode in nonElseCases) {
            val case = TypeInferenceUtils.inferAs<CaseNode, IType.Case>(caseNode, nEnv)

            if (!TypeUtils.checkEq(nEnv, case, expectedCase)) {
                throw invocation.make<TypeSystem>("Case $case does not match expected $expectedCase in Select expression", caseNode)
            }
        }

        val elseType = TypeInferenceUtils.infer(elseCases[0], nEnv) as IType.Case

        if (!TypeUtils.checkEq(nEnv, elseType, expectedCase)) {
            throw invocation.make<TypeSystem>("Else case expected to return $typeAnnotation, found $elseType", elseCases[0])
        }

        return typeAnnotation
    }

    override fun infer(node: SelectNode, env: AnnotatedSelfTypeEnvironment): AnyType {
        val typeAnnotation = env.typeAnnotation
        val cType = TypeInferenceUtils.infer(node.condition, env)
        val conditionType = cType.flatten(cType, env)

        return when (conditionType.getCardinality()) {
            is ITypeCardinality.Finite -> inferFiniteType(node, conditionType, typeAnnotation, env)
            is ITypeCardinality.Infinite -> inferInfiniteType(node, conditionType, typeAnnotation, env)
            else -> TODO("UNSUPPORTED CARDINALITY FOR CASE ANALYSIS")
        }
    }
}