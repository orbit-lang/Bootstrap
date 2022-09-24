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
    override fun infer(node: TypeBindingPatternNode, env: Env): IType<*>
        = TypeSystemUtils.infer(node.typeIdentifier, env)
}

object StructuralPatternInference : ITypeInference<StructuralPatternNode> {
    override fun infer(node: StructuralPatternNode, env: Env): IType<*> {
        val bindingTypes = TypeSystemUtils.inferAll(node.bindings, env)

        return IType.Always
    }
}

object CaseInference : ITypeInference<CaseNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: CaseNode, env: Env): IType<*> {
        val patternType = TypeSystemUtils.infer(node.pattern, env)
        val bodyType = TypeSystemUtils.infer(node.body, env)
        val matchType = env.getMatchType()
        val selfType = env.getSelfType() as? IType.Signature
            ?: throw invocation.make<TypeSystem>("Could not infer `Self` Type in this context", node)

        if (!TypeUtils.checkEq(env, matchType, patternType)) {
            throw invocation.make<TypeSystem>("Case patterns within a Select expression must match the condition type. Expected `${matchType.id}`, found `${patternType.id}`", node.pattern)
        }

        if (!TypeUtils.checkEq(env, bodyType, selfType.returns)) {
            throw invocation.make<TypeSystem>("Case expression expected to return Type `${selfType.returns.id}`, found `${bodyType.id}`", node.body)
        }

        return IType.Arrow1(patternType, bodyType)
    }
}

object SelectInference : ITypeInference<SelectNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: SelectNode, env: Env): IType<*> {
        val conditionType = TypeSystemUtils.infer(node.condition, env)
        val nEnv = env.withMatch(conditionType)
        val caseTypes = TypeSystemUtils.inferAllAs<CaseNode, IType.Arrow1>(node.cases, nEnv)
        val cardinality = caseTypes.fold(ITypeCardinality.Zero as ITypeCardinality) { acc, next -> acc + next.getCardinality() }
        val hasElseCase = node.cases.any { it.pattern is ElseNode }

        if (cardinality is ITypeCardinality.Infinite && !hasElseCase) {
            throw invocation.make<TypeSystem>("Type `${conditionType.id}` has an infinite number of cases and therefore requires an Else case", node)
        }

        // If this is not an Infinite Type, ensure all possible cases are covered
        val coveredCases = caseTypes.map { it.takes }.distinctBy { it.id }

        // TODO - Is this check necessary? Maybe should just a warning?
//        if (cardinality is ITypeCardinality.Mono && coveredCases.count() != 1)

        val conditionCardinality = conditionType.getCardinality()
        if (!hasElseCase && conditionCardinality is ITypeCardinality.Finite && coveredCases.count() != conditionCardinality.count) {
            // TODO - Spit out actual missing cases if possible
            throw invocation.make<TypeSystem>("Missing Cases for Select expression of Type `${conditionType.id}`", node)
        }

        return conditionType
    }
}

object MethodDefInference : ITypeInference<MethodDefNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: MethodDefNode, env: Env): IType<*> {
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
            else -> throw invocation.make<TypeSystem>("Method `${node.signature.identifierNode.identifier}` declared return Type of `${signature.returns.id}`, found `${returnType.id}`", node)
        }
    }
}