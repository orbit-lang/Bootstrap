package org.orbit.types.typeactions

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.AssignmentStatementNode
import org.orbit.core.nodes.TypeProjectionNode
import org.orbit.core.nodes.WhereClauseNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.extensions.annotate
import org.orbit.types.components.*
import org.orbit.types.phase.NominalEqualityConstraint
import org.orbit.types.phase.TraitEnforcer
import org.orbit.util.Invocation
import org.orbit.util.Printer

class TypeProjectionAssembler(private val node: TypeProjectionNode) : TypeAction {
    private companion object : KoinComponent {
        private val invocation: Invocation by inject()
        private val printer: Printer by inject()
    }

    private lateinit var context: Context
    private var type: Type = Type("")
    private lateinit var trait: Trait

    private fun verifyWherePropertyAssignment(assignmentNode: AssignmentStatementNode) {
        val matches = trait.properties.filter { it.name == assignmentNode.identifier.identifier }

        val traitProperty = when (matches.count()) {
            0 -> throw invocation.make<TraitEnforcer>("Cannot project property '${assignmentNode.identifier.identifier}' because Trait ${trait.toString(
                printer
            )} does not declare a property named '${assignmentNode.identifier.identifier}'", assignmentNode)
            1 -> matches.first()
            else -> TODO("???")
        }

        val rhs = TypeInferenceUtil.infer(context, assignmentNode.value) as Entity

//        val eq = traitProperty.equalitySemantics as AnyEquality
        var nProperty = Property(traitProperty.name, rhs, assignmentNode.value)

        val equalityConstraint = NominalEqualityConstraint(traitProperty.type)

        if (!equalityConstraint.checkConformance(context, rhs)) {
            throw invocation.make<TraitEnforcer>(
                "Projected property ${nProperty.toString(printer)} does not match type declared in Trait ${trait.toString(printer)} ${traitProperty.toString(printer)}", assignmentNode)
        }

//        if (!eq.isSatisfied(context, traitProperty.type, rhs)) {
//            throw invocation.make<TraitEnforcer>("Projected property ${nProperty.toString(printer)} does not match type declared in Trait ${trait.toString(
//                printer
//            )} ${traitProperty.toString(printer)}", assignmentNode)
//        }

        nProperty = Property(traitProperty.name, traitProperty.type, assignmentNode.value)

        type = Type(
            type.name,
            type.typeParameters,
            type.properties + nProperty,
            type.traitConformance,
            type.equalitySemantics,
            false,
            typeConstructor = type.typeConstructor
        )

        assignmentNode.annotate(rhs, Annotations.Type)
        assignmentNode.value.annotate(rhs, Annotations.Type)

        context.remove(type.name)
        context.add(type)
    }

    private fun verifyWhereClause(clause: WhereClauseNode) {
        when (clause.whereStatement) {
            is AssignmentStatementNode -> verifyWherePropertyAssignment(clause.whereStatement)
            else -> TODO("???")
        }
    }

    override fun execute(context: Context) {
        this.context = context

        /**
         * Type projections extend the list of trait conformance for the given type.
         * Conformance is verified immediately.
         */
        // TODO - Type check any property/method declarations within the projection
        type = TypeInferenceUtil.infer(context, node.typeIdentifier, null) as Type
        trait = TypeExpressionInference.infer(context, node.traitIdentifier, null) as Trait

        node.typeIdentifier.annotate(type, Annotations.Type)

        type = Type(
            type.name,
            type.typeParameters,
            type.properties,
            type.traitConformance + trait,
            type.equalitySemantics,
            false,
            typeConstructor = type.typeConstructor
        )

        node.whereNodes
            .forEach(::verifyWhereClause)

        val enforcer = TraitConformance(type, trait)

        enforcer.execute(context)

        context.replace(type)

        //if (node.typeIdentifier is MetaTypeNode) {
            // NOTE - This feels dirty!!
            context.replaceMonomorphisedType(type)
        //}

        val typeProjection = TypeProjection(type, trait)

        node.annotate(typeProjection, Annotations.Type)

        context.add(typeProjection)
    }

    override fun describe(printer: Printer): String {
        return "Type projection: ${type.toString(printer)} => ${trait.toString(printer)}"
    }
}