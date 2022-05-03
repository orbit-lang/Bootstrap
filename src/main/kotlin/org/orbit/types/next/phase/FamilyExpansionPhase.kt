package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.FamilyConstructorNode
import org.orbit.core.nodes.FamilyNode
import org.orbit.types.next.components.*
import org.orbit.util.Invocation

object FamilyExpansionPhase : TypePhase<FamilyNode, TypeFamily<*>>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<FamilyNode>): TypeFamily<*> {
        val family = input.inferenceUtil.inferAs<FamilyNode, TypeFamily<*>>(input.node)

        family.members.forEach {
            input.inferenceUtil.addConformance(it, family)

            val alias = Path(family.getPath(OrbitMangler).last()) + it.getPath(OrbitMangler).last()

            input.inferenceUtil.declare(Alias(alias, it))
        }

        return family
    }
}

object FamilyConstructorExpansionPhase : TypePhase<FamilyConstructorNode, PolymorphicType<TypeFamily<PolymorphicType<Type>>>>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<FamilyConstructorNode>): PolymorphicType<TypeFamily<PolymorphicType<Type>>> {
        val family = input.inferenceUtil.inferAs<FamilyConstructorNode, PolymorphicType<TypeFamily<PolymorphicType<Type>>>>(input.node)

        family.baseType.members.forEach {
            input.inferenceUtil.addConformance(it, family.baseType)

            val alias = Path(family.getPath(OrbitMangler).last()) + it.getPath(OrbitMangler).last()

            input.inferenceUtil.declare(Alias(alias, it))
        }

        return family
    }
}