package org.orbit.types.typeresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.getPath
import org.orbit.core.nodes.MetaTypeNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.graph.extensions.annotate
import org.orbit.types.components.*
import org.orbit.util.Invocation
import org.orbit.util.partial

class TypeExpressionTypeResolver(override val node: TypeExpressionNode, override val binding: Binding) : TypeResolver<TypeExpressionNode, TypeExpression>,
    KoinComponent {
    override val invocation: Invocation by inject()

    override fun resolve(environment: Environment, context: Context) : TypeExpression = when (node) {
        is TypeIdentifierNode -> {
            val abstractType = context.getTypeByPath(node.getPath())

            val type = when (abstractType) {
                is TypeConstructor -> MetaType(abstractType, emptyList(), abstractType.properties)
                else -> abstractType as Entity
            }

            node.annotate(type, Annotations.Type)

            type
        }

        is MetaTypeNode -> {
            val type = MetaTypeInference.infer(context, node, null) as TypeExpression
//            val typeConstructor = context.getTypeByPath(node.getPath()) as EntityConstructor
//            // TODO - Convert this to a stream
//            val typeParameters = node.typeParameters
//                .map(partial(::TypeExpressionTypeResolver, binding))
//                .map(partial(TypeExpressionTypeResolver::resolve, environment, context))
//                .map(partial(TypeExpression::evaluate, context))
//                .map { it as ValuePositionType }
//
//            val type = MetaType(typeConstructor, typeParameters, typeConstructor.properties)

            node.annotate(type, Annotations.Type)

            type
        }

        else -> TODO("Unsupported type expression $node")
    }
}