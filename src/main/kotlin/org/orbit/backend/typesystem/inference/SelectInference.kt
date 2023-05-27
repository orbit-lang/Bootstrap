package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatformTools
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.components.Enum
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.CaseNode
import org.orbit.core.nodes.SelectNode
import org.orbit.util.Invocation
import org.orbit.util.cartesianProduct

interface IPatternMatcher<P: AnyType> {
    fun match(env: AnnotatedSelfTypeEnvironment, conditionType: P, patterns: List<Case>, elseCase: ElseCase?)
}

object EnumPatternMatcher : IPatternMatcher<Enum>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun match(env: AnnotatedSelfTypeEnvironment, conditionType: Enum, patterns: List<Case>, elseCase: ElseCase?) {
        val distinctPatterns = mutableListOf<Case>()
        for (pattern in patterns) {
            if (distinctPatterns.contains(pattern)) {
                invocation.make<TypeSystem>("Duplicate pattern found in Select expression: $pattern")
            }

            distinctPatterns.add(pattern.eraseResult())
        }

        val requiredPatterns = conditionType.cases.map {
            Case(it, env.typeAnnotation)
        }

        val missingCases = requiredPatterns.toMutableList()
        for (pattern in requiredPatterns) {
            if (distinctPatterns.any { it.id == pattern.id }) {
                missingCases.remove(pattern)
            }
        }

        when (missingCases.isEmpty()) {
            true -> {}
            else -> when (elseCase) {
                null -> {
                    val pretty = missingCases.joinToString("\n\t")

                    throw invocation.make<TypeSystem>("Missing ${missingCases.count()}/${requiredPatterns.count()} Case(s) for Select expression of Type $conditionType:\n\t$pretty")
                }

                else -> when (TypeUtils.checkEq(env, elseCase.result, env.typeAnnotation)) {
                    true -> {}
                    else -> throw invocation.make<TypeSystem>("Else Case result Type does not match expected result. Found ${elseCase.result}, expected ${env.typeAnnotation}")
                }
            }
        }
    }
}

object TuplePatternMatcher : IPatternMatcher<Tuple>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun match(env: AnnotatedSelfTypeEnvironment, conditionType: Tuple, patterns: List<Case>, elseCase: ElseCase?) {
        val lType = conditionType.left
        val rType = conditionType.right

        if (lType !is Enum) TODO("Tuples of Non-Enum Type")
        if (rType !is Enum) TODO("Tuples of Non-Enum Type")

        val requiredCases = lType.cases.cartesianProduct(rType.cases)
            .map { Case(Tuple(it.first, it.second), env.typeAnnotation) }
            .toList()
        val distinctCases = patterns.distinct().map { it.eraseResult() }
        val missingCases = requiredCases.toMutableList()
        for (pattern in requiredCases) {
            if (distinctCases.any { it.id == pattern.id }) {
                missingCases.remove(pattern)
            }
        }

        when (missingCases.isEmpty()) {
            true -> {}
            else -> when (elseCase) {
                null -> {
                    val pretty = missingCases.joinToString("\n\t")

                    throw invocation.make<TypeSystem>("Missing ${missingCases.count()}/${requiredCases.count()} Case(s) for Select expression of Type $conditionType:\n\t$pretty")
                }

                else -> when (TypeUtils.checkEq(env, elseCase.result, env.typeAnnotation)) {
                    true -> {}
                    else -> throw invocation.make<TypeSystem>("Else Case result Type does not match expected result. Found ${elseCase.result}, expected ${env.typeAnnotation}")
                }
            }
        }
    }
}

object PatternMatchUtil {
    fun match(env: AnnotatedSelfTypeEnvironment, conditionType: AnyType, patterns: List<Case>, elseCase: ElseCase?) {
        val matcher = KoinPlatformTools.defaultContext().get().get<IPatternMatcher<AnyType>>(named("match${conditionType::class.java.simpleName}"))

        matcher.match(env, conditionType, patterns, elseCase)
    }
}

object SelectInference : ITypeInference<SelectNode, AnnotatedSelfTypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

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
            throw invocation.make<TypeSystem>("Multiple Else cases found in Select expression", elseNodes[1])
        }

        val elseCase = when (elseNodes.isEmpty()) {
            true -> null
            else -> ElseCase(TypeInferenceUtils.infer(elseNodes[0].body, env))
        }

        val cases = TypeInferenceUtils.inferAllAs<CaseNode, Case>(nonElseCases, nEnv)

        PatternMatchUtil.match(env, constructableType, cases, elseCase)

        return when (node.inSingleExpressionPosition) {
            true -> typeAnnotation
            else -> TODO("UNIFY CASES")
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