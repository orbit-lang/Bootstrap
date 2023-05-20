package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUnificationUtil
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.CaseNode
import org.orbit.core.nodes.SelectNode
import org.orbit.util.Invocation

object SelectInference : ITypeInference<SelectNode, AnnotatedSelfTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    private fun unify(env: ITypeEnvironment, cases: List<Case>) : AnyType {
        val codomains = cases.map { it.getCodomain() }
            .distinct()

        return when (codomains.count()) {
            0 -> TODO("???")
            1 -> codomains[0]
            else -> codomains.reduce { acc, next -> TypeUnificationUtil.unify(env, acc, next) }
        }
    }

    private fun inferFiniteType(node: SelectNode, conditionType: AnyType, typeAnnotation: AnyType, env: AnnotatedSelfTypeEnvironment) : AnyType {
        val constructableType = conditionType as? IConstructableType<*>
            ?: throw invocation.compilerError<TypeSystem>("Finite non-constructable type?!", node)

        val nEnv = when (node.inSingleExpressionPosition) {
            true -> CaseTypeEnvironment(env, env.getSelfType(), conditionType)
            else -> CaseTypeEnvironment(env, Always, conditionType)
        }

        val nonElseCases = node.cases.filterNot { it.isElseCase }
        val elseNodes = node.cases.filter { it.isElseCase }

        if (elseNodes.count() > 1) {
            throw invocation.make<TypeSystem>("Multiple Else cases found in Select expression", elseNodes.last())
        }

        val hasElseCase = elseNodes.count() == 1

        if (conditionType.getCardinality() == ITypeCardinality.Infinite && !hasElseCase) {
            throw invocation.make<TypeSystem>("Type `$constructableType` has an infinite number of cases and therefore requires an Else case", node)
        }

        val requiredConstructors = constructableType.getConstructors()
        val providedCases = TypeInferenceUtils.inferAllAs<CaseNode, Case>(nonElseCases, nEnv)
        val providedConstructors = providedCases
            .flatMap { it.condition.getConstructors() }

        val missingConstructors = requiredConstructors.toMutableList()
        for (requiredConstructor in requiredConstructors) {
            for (providedConstructor in providedConstructors) {
                if (TypeUtils.checkEq(nEnv, providedConstructor, requiredConstructor)) {
                    missingConstructors.remove(requiredConstructor)
                }
            }
        }

        if (missingConstructors.isNotEmpty()) {
            val prettyMissing = missingConstructors.joinToString("\n\t")

            throw invocation.make<TypeSystem>("Missing ${missingConstructors.count()}/${requiredConstructors.count()} Case(s) for Select expression of Type `$constructableType`:\n\t$prettyMissing", node)
        }

        return when (node.inSingleExpressionPosition) {
            true -> typeAnnotation
            else -> unify(env, providedCases)
        }
    }

    private fun inferInfiniteType(node: SelectNode, conditionType: AnyType, typeAnnotation: AnyType, env: AnnotatedSelfTypeEnvironment) : AnyType {
        val elseCases = node.cases.filter { it.isElseCase }
        val nEnv = when (node.inSingleExpressionPosition) {
            true -> CaseTypeEnvironment(env, env.getSelfType(), conditionType)
            else -> CaseTypeEnvironment(env, Always, conditionType)
        }

        if (elseCases.count() > 1) {
            throw invocation.make<TypeSystem>("Multiple Else cases found in Select expression", elseCases.last())
        }

        if (elseCases.isEmpty()) {
            throw invocation.make<TypeSystem>("Select expression must contain an Else case where the condition Type (AKA $conditionType) has Infinite Cardinality", node)
        }

        val nonElseCases = node.cases.filterNot { it.isElseCase }
        val expectedCase = Case(conditionType, typeAnnotation)

        for (caseNode in nonElseCases) {
            val case = TypeInferenceUtils.inferAs<CaseNode, Case>(caseNode, nEnv)

            if (!TypeUtils.checkEq(nEnv, case, expectedCase)) {
                throw invocation.make<TypeSystem>("Case $case does not match expected $expectedCase in Select expression", caseNode)
            }
        }

        val elseType = TypeInferenceUtils.infer(elseCases[0], nEnv) as Case

        if (!TypeUtils.checkEq(nEnv, elseType, expectedCase)) {
            throw invocation.make<TypeSystem>("Else case expected to return $typeAnnotation, found $elseType", elseCases[0])
        }

        return typeAnnotation
    }

    override fun infer(node: SelectNode, env: AnnotatedSelfTypeEnvironment): AnyType {
        val typeAnnotation = env.typeAnnotation
        val cType = TypeInferenceUtils.infer(node.condition, env)
        val conditionType = cType.flatten(cType, env)

        if (conditionType is IValue<*, *>) {
            throw invocation.make<TypeSystem>("Selecting on a constant value always results in the same result and is therefore prohibited: $conditionType", node.condition)
        }

        return when (conditionType.getCardinality()) {
            is ITypeCardinality.Finite -> inferFiniteType(node, conditionType, typeAnnotation, env)
            is ITypeCardinality.Infinite -> inferInfiniteType(node, conditionType, typeAnnotation, env)
            else -> TODO("UNSUPPORTED CARDINALITY FOR CASE ANALYSIS")
        }
    }
}