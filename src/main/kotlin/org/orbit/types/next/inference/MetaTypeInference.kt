package org.orbit.types.next.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.MetaTypeNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.types.next.components.*
import org.orbit.util.Printer

object MetaTypeInference : ITypeExpressionInference<MetaTypeNode, MonomorphicType<*>>, KoinComponent {
    private val printer: Printer by inject()

    override fun infer(inferenceUtil: InferenceUtil, context: InferenceContext, node: MetaTypeNode): InferenceResult {
        val polyType = inferenceUtil.inferAs<TypeIdentifierNode, PolymorphicType<TypeComponent>>(node.typeConstructorIdentifier)
        val parameters = inferenceUtil.inferAllAs<TypeExpressionNode, TypeComponent>(node.typeParameters,
            AnyInferenceContext(TypeExpressionNode::class.java)
        ).mapIndexed { idx, type -> Pair(idx, type) }

        return when (polyType.baseType) {
            is Type -> TypeMonomorphiser.monomorphise(
                inferenceUtil.toCtx(),
                polyType as PolymorphicType<FieldAwareType>,
                parameters,
                MonomorphisationContext.Any
            ).toInferenceResult(printer)

            is Trait -> TraitMonomorphiser.monomorphise(
                inferenceUtil.toCtx(),
                polyType as PolymorphicType<ITrait>,
                parameters,
                MonomorphisationContext.TraitConformance(inferenceUtil.self)
            ).toInferenceResult(printer)

            is TypeFamily<*> -> FamilyMonomorphiser.monomorphise(
                inferenceUtil.toCtx(),
                polyType as PolymorphicType<TypeFamily<*>>,
                parameters,
                MonomorphisationContext.Any
            ).toInferenceResult(printer)

            else -> InferenceResult.Failure(Never("Cannot specialise Polymorphic Type ${polyType.toString(printer)}"))
        }
    }
}