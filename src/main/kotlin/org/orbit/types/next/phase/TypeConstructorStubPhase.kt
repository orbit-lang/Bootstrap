package org.orbit.types.next.phase

import org.koin.core.component.inject
import org.orbit.core.getPath
import org.orbit.core.nodes.PairNode
import org.orbit.core.nodes.TypeConstructorNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.types.next.components.Field
import org.orbit.types.next.components.Parameter
import org.orbit.types.next.components.PolymorphicType
import org.orbit.types.next.components.Type
import org.orbit.types.next.inference.AnyInferenceContext
import org.orbit.types.next.inference.TypeLiteralInferenceContext
import org.orbit.util.Invocation

object TypeConstructorStubPhase : EntityConstructorStubPhase<TypeConstructorNode, Type> {
    override val invocation: Invocation by inject()

    override fun run(input: TypePhaseData<TypeConstructorNode>): PolymorphicType<Type> {
        val parameters = input.inferenceUtil.inferAllAs<TypeIdentifierNode, Parameter>(input.node.typeParameterNodes,
            TypeLiteralInferenceContext.TypeParameterContext
        )

        parameters.forEach { input.inferenceUtil.declare(it) }

        val fields = input.inferenceUtil.inferAllAs<PairNode, Field>(input.node.properties,
            AnyInferenceContext(PairNode::class.java)
        )
        val baseType = Type(input.node.getPath(), fields)

        return PolymorphicType(baseType, parameters)
    }
}