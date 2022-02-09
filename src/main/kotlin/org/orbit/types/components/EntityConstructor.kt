package org.orbit.types.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.nodes.WhereClauseTypeBoundsExpressionNode
import org.orbit.types.phase.TypeSystem
import org.orbit.types.util.TraitConstructorMonomorphisation
import org.orbit.types.util.TypeMonomorphisation
import org.orbit.util.Invocation
import org.orbit.util.Printer

object TraitMergeTool : KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    fun merge(context: Context, traitA: Trait, traitB: Trait) : Trait {
        val nTraitName = "${traitA.name}_${traitB.name}"
        val nTraitConformance = traitA.traitConformance.toSet()
            .plus(traitB.traitConformance)
            .distinct()

        val nProperties = mutableListOf<Property>()
        for (p1 in traitA.properties) {
            for (p2 in traitB.properties) {
                if (p1.name == p2.name) {
                    val equalA = p1.type.isSatisfied(context, p2.type)
                    val equalB = p2.type.isSatisfied(context, p1.type)

                    if (equalA && !equalB) {
                        nProperties.add(p1)
                    } else if (!equalA && equalB) {
                        nProperties.add(p2)
                    } else if (equalA && equalB) {
                        nProperties.add(p1)
                    } else {
                        throw invocation.make("Traits ${traitA.toString(printer)} & ${traitB.toString(printer)} are in conflict regarding the properties ${p1.toString(printer)} & ${p2.toString(printer)}, and therefore may not be used in conjunction in the context of an Extension's Where clauses")
                    }
                } else {
                    nProperties.add(p2)
                }
            }
        }

        val nSignatures = mutableListOf<SignatureProtocol<*>>()
        for (s1 in traitA.signatures) {
            for (s2 in traitB.signatures) {
                if (s1.name == s2.name) {
                    val equalA = s1.isSatisfied(s2, context)
                    val equalB = s2.isSatisfied(s1, context)

                    if (equalA && !equalB) {
                        nSignatures.add(s1)
                    } else if (!equalA && equalB) {
                        nSignatures.add(s2)
                    } else if (equalA && equalB) {
                        throw invocation.make("Traits ${traitA.toString(printer)} & ${traitB.toString(printer)} are in conflict regarding the signatures ${s1.toString(printer)} & ${s2.toString(printer)}, and therefore may not be used in conjunction in the context of an Extension's Where clauses")
                    }
                } else {
                    nSignatures.add(s2)
                }
            }
        }

        return Trait(nTraitName, properties = nProperties, signatures = nSignatures, traitConformance = nTraitConformance, isEphemeral = true)
    }
}

data class ConstrainedEntityConstructor(val entityConstructor: EntityConstructor, val concreteTypeParameters: List<Pair<Int, Entity>>) {
    private fun isComplete() : Boolean
        = concreteTypeParameters.count() == entityConstructor.typeParameters.count()

    fun removeTypeParameter(index: Int) : ConstrainedEntityConstructor {
        val nTypeParameters = concreteTypeParameters.filterNot { it.first == index }

        return ConstrainedEntityConstructor(entityConstructor, nTypeParameters)
    }

    fun resolveTypeParameter(index: Int, concreteType: Entity) : ConstrainedEntityConstructor {
        return ConstrainedEntityConstructor(entityConstructor, concreteTypeParameters + Pair(index, concreteType))
    }

    fun synthesise(context: Context) : Entity {
        var concreteTypes = concreteTypeParameters.sortedBy { it.first }

        concreteTypes = when (isComplete()) {
            true -> concreteTypes
            false -> {
                val offset = entityConstructor.typeParameters.count() - concreteTypes.count()

                concreteTypes + IntRange(0, offset).map { Pair(it, IntrinsicTypes.AnyType.type as Entity) }
            }
        }

        val nTypes = concreteTypes.map { when (it.second) {
            is Type -> it.second
            is Trait -> (it.second as Trait).synthesise(true)
            else -> TODO("@EntityConstructor:93")
        }}

        val specialist = when (entityConstructor) {
            is TypeConstructor -> TypeMonomorphisation(entityConstructor, nTypes, omitTypeParameters = false)
            is TraitConstructor -> TraitConstructorMonomorphisation(entityConstructor, nTypes)
            else -> TODO("@EntityConstructor:98")
        }

        return specialist.specialise(context)
    }
}

interface EntityConstructorConstraint {
    fun refine(context: Context, node: WhereClauseTypeBoundsExpressionNode, wrapper: ConstrainedEntityConstructor, typeParameter: TypeParameter, index: Int, concreteType: Entity) : ConstrainedEntityConstructor
}

object EqualityBoundsConstraint : EntityConstructorConstraint, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun refine(context: Context, node: WhereClauseTypeBoundsExpressionNode, wrapper: ConstrainedEntityConstructor, typeParameter: TypeParameter, index: Int, concreteType: Entity): ConstrainedEntityConstructor {
        if (concreteType !is Type)
            throw invocation.make<TypeSystem>("Traits cannot be the target of equality type constraints, use `Self[${typeParameter.name}] : ${concreteType.toString(printer)}` instead", node.targetTypeExpression)

        val previous = wrapper.concreteTypeParameters.firstOrNull { it.first == index }

        if (previous != null) {
            // This is an error because it is not possible for a Type Parameter
            // to inhabit 2 or more concrete Types, e.g:
            //  `extension List where Self[Element] = Int where Self[Element] = String`
            throw invocation.make<TypeSystem>("Attempting to constraint a Type Parameter ${typeParameter.toString(printer)} to multiple concrete types: ${previous.second.toString(printer)} & ${concreteType.toString(printer)}", node.targetTypeExpression)
        }

        return wrapper.resolveTypeParameter(index, concreteType)
    }
}

object ConformanceBoundsConstraint : EntityConstructorConstraint, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun refine(context: Context, node: WhereClauseTypeBoundsExpressionNode, wrapper: ConstrainedEntityConstructor, typeParameter: TypeParameter, index: Int, concreteType: Entity): ConstrainedEntityConstructor {
        val concreteTrait = concreteType as? Trait
            ?: throw invocation.make<TypeSystem>("The right-hand side of a Conformance Bound must resolve to a Trait, found: ${concreteType.toString(printer)}", node.targetTypeExpression)

        val previous = wrapper.concreteTypeParameters.firstOrNull { it.first == index }
            ?: return wrapper.resolveTypeParameter(index, concreteType)

        val previousTrait = previous.second as? Trait
            ?: throw invocation.make<TypeSystem>("Mixing Trait Conformance & Type Equality constraints is not supported. Conflict found between ${concreteTrait.toString(printer)} & ${previous.second.toString(printer)}", node.targetTypeExpression)

        val nTrait = TraitMergeTool.merge(context, previousTrait, concreteTrait)
        val nType = nTrait.synthesise(true)

        context.registerSyntheticTrait(nTrait)

        return wrapper.removeTypeParameter(previous.first)
            .resolveTypeParameter(index, nType)
    }
}

class TypeParameterBoundsConstraint(
    private val node: WhereClauseTypeBoundsExpressionNode,
    private val typeParameter: TypeParameter,
    private val concreteType: Entity
) : KoinComponent {
    fun refine(context: Context, wrapper: ConstrainedEntityConstructor): ConstrainedEntityConstructor {
        val idx = wrapper.entityConstructor.typeParameters.indexOf(typeParameter)

        // We check this further up, so shouldn't really be necessary - but just in case!
        if (idx < 0) return wrapper

        return node.boundsType.entityConstructorConstraint.refine(context, node, wrapper, typeParameter, idx, concreteType)
    }
}

interface EntityConstructor : PropertyProvidingType, EntityProtocol {
    val typeParameters: List<TypeParameter>
    override val properties: List<Property>
    val partiallyResolvedTraitConstructors: List<PartiallyResolvedTraitConstructor>

    fun getTypeParameterOrNull(path: Path) : TypeParameter? {
        return typeParameters.find { OrbitMangler.unmangle(it.name) == path }
    }
}