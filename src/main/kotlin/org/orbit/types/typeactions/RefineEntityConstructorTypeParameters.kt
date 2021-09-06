package org.orbit.types.typeactions

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.EntityConstructorNode
import org.orbit.core.nodes.EntityConstructorWhereClauseNode
import org.orbit.core.nodes.TypeConstraintNode
import org.orbit.types.components.*
import org.orbit.types.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.partial

class RefineEntityConstructorTypeParameters(private val node: EntityConstructorNode) : TypeAction {
    private companion object : KoinComponent {
        private val invocation: Invocation by inject()
        private val printer: Printer by inject()
    }

    private lateinit var entityConstructor: EntityConstructor

    private fun resolveTypeConstraint(typeConstraintNode: TypeConstraintNode, context: Context) {
        val constraintTrait = context.getTypeByPath(typeConstraintNode.constraintTraitNode.getPath()) as Trait
        val constrainedTypePath = typeConstraintNode.constrainedTypeNode.getPath()

        val idx = entityConstructor.typeParameters.indexOfFirst { it.name == constrainedTypePath.toString(OrbitMangler) }

        if (idx == -1) throw invocation.make<TypeSystem>("Entity Constructor ${entityConstructor.toString(printer)} does not declare a Type Parameter named '${typeConstraintNode.constraintTraitNode.value}'", typeConstraintNode.constrainedTypeNode)

        val originalTypeParameter = entityConstructor.typeParameters[idx]

        if (originalTypeParameter.constraints.contains(constraintTrait)) {
            throw invocation.make<TypeSystem>("Type Parameter ${originalTypeParameter.toString(printer)} is already constrained by Trait ${constraintTrait.toString(printer)}", typeConstraintNode.constrainedTypeNode)
        }

        val allTypeParameters = entityConstructor.typeParameters.toMutableList()

        allTypeParameters.removeAt(idx)

        val nTypeParameter = TypeParameter(originalTypeParameter.name, originalTypeParameter.constraints + constraintTrait)

        allTypeParameters.add(idx, nTypeParameter)

        entityConstructor = when (entityConstructor) {
            is TypeConstructor -> TypeConstructor(entityConstructor.name, allTypeParameters, entityConstructor.properties)
            else -> TraitConstructor(entityConstructor.name, allTypeParameters, entityConstructor.properties)
        }

        context.remove(entityConstructor.name)
        context.add(entityConstructor)
    }

    private fun resolveClause(clause: EntityConstructorWhereClauseNode, context: Context) = when (clause.statementNode) {
        is TypeConstraintNode -> resolveTypeConstraint(clause.statementNode, context)
        else -> TODO("RefineEntityConstructorTypeParameters::resolveClause")
    }

    override fun execute(context: Context) {
        entityConstructor = context.getTypeByPath(node.getPath()) as EntityConstructor

        node.clauses.forEach(partial(::resolveClause, context))
    }

    override fun describe(printer: Printer): String {
        return "Refine Type Parameters for Entity Constructor ${entityConstructor.toString(printer)}"
    }
}