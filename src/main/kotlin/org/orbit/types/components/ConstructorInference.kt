package org.orbit.types.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.ConstructorNode
import org.orbit.core.nodes.MetaTypeNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.extensions.annotate
import org.orbit.types.phase.AnyEqualityConstraint
import org.orbit.types.phase.TypeSystem
import org.orbit.types.util.TypeMonomorphisation
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.pluralise

class TypeConstructorTypeParameterInference(private val typeConstructor: TypeConstructor, private val propertyTypes: List<Pair<Property, TypeProtocol>>) {
    fun infer(context: ContextProtocol) : ValuePositionType? {
        // TODO - Better error message
        assert(propertyTypes.count() == typeConstructor.properties.count())

        val zipped = typeConstructor.properties.zip(propertyTypes)
        val requiredMatches = typeConstructor.typeParameters.count()
        val matches = mutableListOf<ValuePositionType>()
        for (typeParameter in typeConstructor.typeParameters) {
            for (pair in zipped) {
                if (typeParameter.name == pair.first.type.name) {
                    matches.add(pair.second.second as ValuePositionType)
                }
            }

            if (matches.count() == requiredMatches) break
        }

        if (matches.count() != requiredMatches) return null

        val specialist = TypeMonomorphisation(typeConstructor, matches)

        return specialist.specialise(context)
    }
}

object ConstructorInference : TypeInference<ConstructorNode>, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun infer(context: Context, node: ConstructorNode, typeAnnotation: TypeProtocol?): TypeProtocol {
        val receiverType = TypeInferenceUtil.infer(context, node.typeExpressionNode)

        if (receiverType !is PropertyProvidingType) {
            throw invocation.make<TypeSystem>(
                "Only concrete types may be initialised via a constructor call. Found ${receiverType::class.java.simpleName} ${receiverType.toString(printer)}",
                node.typeExpressionNode
            )
        }

        var parameterTypes = receiverType.properties.mapNotNull {
             when (it.defaultValue) {
                 null -> it
                 else -> null
             }
        }.toMutableList()

        val argumentTypes = node.parameterNodes.map { TypeInferenceUtil.infer(context, it) }

        if (node.parameterNodes.size != parameterTypes.size) {
            throw invocation.make<TypeSystem>("Type '${receiverType.name}' expects ${parameterTypes.size} constructor ${"parameter".pluralise(parameterTypes.size)}, found ${node.parameterNodes.size}", node.firstToken.position)
        }

        val fReceiverType: Type = when (receiverType) {
            is Type -> receiverType
            is TypeConstructor -> {
                val inf = TypeConstructorTypeParameterInference(receiverType, receiverType.properties.zip(argumentTypes))

                inf.infer(context) as Type
            }

            else -> TODO("")
        }

        node.typeExpressionNode.annotate(fReceiverType, Annotations.Type)

        parameterTypes = fReceiverType.properties.toMutableList()

        for ((idx, pair) in parameterTypes.zip(node.parameterNodes).withIndex()) {
            val argumentType = TypeInferenceUtil.infer(context, pair.second)
            val equalityConstraint = AnyEqualityConstraint(pair.first.type)

            if (!equalityConstraint.checkConformance(context, argumentType)) {
                throw invocation.make<TypeSystem>("Constructor expects parameter of type ${pair.first.type.toString(printer)} at position ${idx}, found ${argumentType.toString(printer)}", pair.second.firstToken.position)
            }

            parameterTypes[idx] = Property(pair.first.name, argumentType)
        }

        if (node.typeExpressionNode is MetaTypeNode) {
            // TODO - this is super dirty!!!
            val nType = Type(receiverType.name, properties = parameterTypes, typeParameters = fReceiverType.typeParameters, traitConformance = fReceiverType.traitConformance, equalitySemantics = fReceiverType.equalitySemantics, isRequired = fReceiverType.isRequired, isEphemeral = fReceiverType.isEphemeral,typeConstructor = fReceiverType.typeConstructor)

            context.registerMonomorphisation(nType)

            node.typeExpressionNode.annotate(nType, Annotations.Type, true)

            return nType
        }

        return fReceiverType
    }
}