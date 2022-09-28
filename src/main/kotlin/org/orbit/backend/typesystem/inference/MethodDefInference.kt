package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.nodes.*
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.components.ITypeCardinality
import org.orbit.precess.backend.utils.TypeUtils
import org.orbit.util.Invocation

object TypeBindingPatternInference : ITypeInference<TypeBindingPatternNode> {
    override fun infer(node: TypeBindingPatternNode, env: Env): AnyType
        = TypeSystemUtils.infer(node.typeIdentifier, env)
}

object StructuralPatternInference : ITypeInference<StructuralPatternNode> {
    override fun infer(node: StructuralPatternNode, env: Env): AnyType {
        val bindingTypes = TypeSystemUtils.inferAll(node.bindings, env)

        return IType.Always
    }
}

object CaseInference : ITypeInference<CaseNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: CaseNode, env: Env): AnyType {
        val patternType = TypeSystemUtils.infer(node.pattern, env)
        val bodyType = TypeSystemUtils.infer(node.body, env)
        val matchType = env.getMatchType()
        val selfType = env.getSelfType() as? IType.Signature
            ?: throw invocation.make<TypeSystem>("Could not infer `Self` Type in this context", node)

        if (!TypeUtils.checkEq(env, matchType, patternType)) {
            throw invocation.make<TypeSystem>("Case patterns within a Select expression must match the condition type. Expected `$matchType`, found `$patternType`", node.pattern)
        }

        if (!TypeUtils.checkEq(env, bodyType, selfType.returns)) {
            throw invocation.make<TypeSystem>("Case expression expected to return Type `${selfType.returns}`, found `$bodyType`", node.body)
        }

        return IType.Case(patternType, bodyType)
    }
}

object SelectInference : ITypeInference<SelectNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: SelectNode, env: Env): AnyType {
        val typeAnnotation = TypeSystemUtils.popTypeAnnotation()
            ?: TODO("CANNOT INFER TYPE ANNOTATION")

        val conditionType = TypeSystemUtils.infer(node.condition, env)
        val nEnv = env.withMatch(conditionType)
        val caseTypes = TypeSystemUtils.inferAllAs<CaseNode, IType.Case>(node.cases, nEnv)
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
//
        if (!hasElseCase && conditionCardinality is ITypeCardinality.Finite && actualCases.count() != conditionCardinality.count) {
            val missingCases = expectedCases.filterNot { it.id in coveredCases }
            val prettyMissing = missingCases.joinToString("\n\t")
//
//            // TODO - Spit out actual missing cases if possible
            throw invocation.make<TypeSystem>("Missing ${missingCases.count()}/${expectedCases.count()} Case(s) for Select expression of Type `$constructableType`:\n\t$prettyMissing", node)
        }

        return typeAnnotation
    }
}

object MethodDefInference : ITypeInference<MethodDefNode>, KoinComponent {
    private val invocation: Invocation by inject()

    @Suppress("NAME_SHADOWING")
    override fun infer(node: MethodDefNode, env: Env): AnyType {
        val env = when (val n = node.context) {
            null -> env
            else -> env + TypeSystemUtils.inferAs(n, env)
        }

        val signature = TypeSystemUtils.inferAs<MethodSignatureNode, IType.Signature>(node.signature, env, parametersOf(false))

        TypeSystemUtils.pushTypeAnnotation(signature.returns)

        val nEnv = env.extend(Decl.Clone())
            .withSelf(signature)

        if (node.signature.isInstanceMethod) {
            val decl = Decl.Assignment(node.signature.receiverIdentifier!!.identifier, signature.receiver)

            nEnv.extendInPlace(decl)
        }

        if (node.signature.parameterNodes.isNotEmpty()) {
            val pDecl = node.signature.getAllParameterPairs().map {
                Decl.Assignment(it.identifierNode.identifier, TypeSystemUtils.infer(it.typeExpressionNode, nEnv))
            }
            .reduce(Decl::plus)

            nEnv.extendInPlace(pDecl)
        }

        val returnType = TypeSystemUtils.infer(node.body, nEnv)

        return when (TypeUtils.checkEq(nEnv, returnType, signature.returns)) {
            true -> signature
            else -> throw invocation.make<TypeSystem>("Method `${node.signature.identifierNode.identifier}` declared return Type of `${signature.returns}`, found `${returnType}`", node)
        }
    }
}