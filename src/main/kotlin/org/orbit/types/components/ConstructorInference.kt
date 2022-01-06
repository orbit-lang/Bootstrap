package org.orbit.types.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.ConstructorNode
import org.orbit.core.nodes.MetaTypeNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.extensions.annotate
import org.orbit.types.phase.AnyEqualityConstraint
import org.orbit.types.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.pluralise

object ConstructorInference : TypeInference<ConstructorNode>, KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun infer(context: Context, node: ConstructorNode, typeAnnotation: TypeProtocol?): TypeProtocol {
        val receiverType = TypeInferenceUtil.infer(context, node.typeExpressionNode)

        node.typeExpressionNode.annotate(receiverType, Annotations.Type)

        if (receiverType !is Type) {
            throw invocation.make<TypeSystem>(
                "Only concrete types may be initialised via a constructor call. Found ${receiverType::class.java.simpleName} ${receiverType.toString(printer)}",
                node.typeExpressionNode
            )
        }

        val parameterTypes = receiverType.properties.mapNotNull {
             when (it.defaultValue) {
                 null -> it
                 else -> null
             }
        }.toMutableList()

        if (node.parameterNodes.size != parameterTypes.size) {
            throw invocation.make<TypeSystem>("Type '${receiverType.name}' expects ${parameterTypes.size} constructor ${"parameter".pluralise(parameterTypes.size)}, found ${node.parameterNodes.size}", node.firstToken.position)
        }

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
            val nType = Type(receiverType.name, properties = parameterTypes, typeParameters = receiverType.typeParameters, traitConformance = receiverType.traitConformance, equalitySemantics = receiverType.equalitySemantics, isRequired = receiverType.isRequired, isEphemeral = receiverType.isEphemeral,typeConstructor = receiverType.typeConstructor)

            context.registerMonomorphisation(nType)

            node.typeExpressionNode.annotate(nType, Annotations.Type, true)

            return nType
        }

        return receiverType
    }
}