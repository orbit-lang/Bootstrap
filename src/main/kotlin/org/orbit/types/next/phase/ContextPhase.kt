package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.ContextNode
import org.orbit.core.nodes.WhereClauseNode
import org.orbit.types.next.components.Context
import org.orbit.types.next.components.TypeComponent
import org.orbit.types.next.components.TypeVariable
import org.orbit.types.next.inference.AnyInferenceContext
import org.orbit.types.next.components.ITypeConstraint
import org.orbit.util.Invocation

object ContextPhase : TypePhase<ContextNode, Context>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<ContextNode>): Context {
        val typeVariables = input.node.typeVariables.map { TypeVariable(it.getPath().toString(OrbitMangler)) }
        val nInferenceContext = input.inferenceUtil.derive()

        typeVariables.forEach(nInferenceContext::declare)

        val constraints = input.node.clauses.map { nInferenceContext.inferAs<WhereClauseNode, ITypeConstraint<TypeComponent>>(it, AnyInferenceContext(WhereClauseNode::class.java)) }

        return Context(input.node.getPath().toString(OrbitMangler), typeVariables, constraints)
    }
}