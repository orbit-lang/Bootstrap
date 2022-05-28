package org.orbit.types.next.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.PairNode
import org.orbit.core.nodes.TypeConstructorNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.types.next.components.Field
import org.orbit.types.next.components.IType
import org.orbit.types.next.components.PolymorphicType
import org.orbit.types.next.components.Type
import org.orbit.types.next.inference.AnyInferenceContext
import org.orbit.types.next.inference.TypeReference
import org.orbit.util.Invocation
import org.orbit.util.Printer

object TypeFieldsPhase : TypePhase<TypeDefNode, IType>, KoinComponent {
    override val invocation: Invocation by inject()
    private val printer: Printer by inject()

    override fun run(input: TypePhaseData<TypeDefNode>): IType {
        val typeStub = input.inferenceUtil.inferAs<TypeDefNode, IType>(input.node)
        val nInferenceUtil = input.inferenceUtil.derive(retainsTypeMap = true, retainsBindingScope = true, TypeReference(typeStub.fullyQualifiedName))
        val fields = nInferenceUtil.inferAllAs<PairNode, Field>(input.node.propertyPairs, AnyInferenceContext(PairNode::class.java))

        val grouped = fields.groupBy { it.memberName }

        for (group in grouped) {
            if (group.value.count() > 1) {
                val pretty = group.value.joinToString(", ") { it.toString(printer) }

                throw invocation.make<TypeSystem>("Type ${typeStub.toString(printer)} declares multiple parameters with the same name '${group.key}':\n\t$pretty", input.node)
            }
        }

        return Type(typeStub.fullyQualifiedName, fields)
    }
}

object TypeConstructorFieldsPhase : TypePhase<TypeConstructorNode, PolymorphicType<*>>, KoinComponent {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<TypeConstructorNode>): PolymorphicType<*> {
        val stub = input.inferenceUtil.inferAs<TypeConstructorNode, PolymorphicType<*>>(input.node)
        val nInferenceUtil = input.inferenceUtil.derive(self = TypeReference(stub.fullyQualifiedName))
        val fields = input.node.properties.map {
            val type = nInferenceUtil.infer(it.typeExpressionNode)

            Field(it.identifierNode.identifier, type)
        }

        return stub.withPartialFields(fields)
    }
}