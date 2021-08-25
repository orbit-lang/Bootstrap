package org.orbit.types.typeresolvers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.getType
import org.orbit.core.nodes.MethodDefNode
import org.orbit.graph.components.Binding
import org.orbit.graph.components.Environment
import org.orbit.types.components.*
import org.orbit.types.phase.TypeInitialisation
import org.orbit.util.Invocation

class MethodTypeResolver(override val node: MethodDefNode, override val binding: Binding) : TypeResolver<MethodDefNode, SignatureProtocol<out TypeProtocol>>, KoinComponent {
    override val invocation: Invocation by inject()

    constructor(pair: Pair<MethodDefNode, Binding>) : this(pair.first, pair.second)

    override fun resolve(environment: Environment, context: Context) : SignatureProtocol<out TypeProtocol> {
        val parameterBindings = mutableListOf<String>()
        val localContext = Context(context)

        try {
            val signature = node.signature.getType() as SignatureProtocol<TypeProtocol>
            val body = node.body

            signature.parameters.forEach { localContext.bind(it.name, it.type) }

            if (body.isEmpty) {
                // Return type is implied to be Unit, check signature agrees
                val equalitySemantics = signature.returnType.equalitySemantics as AnyEquality
                if (!equalitySemantics.isSatisfied(localContext, signature.returnType, IntrinsicTypes.Unit.type)) {
                    throw invocation.make<TypeInitialisation>("Method '${signature.name}' declares a return type of '${signature.returnType.name}', found 'Unit'", node)
                }
            } else {
                val methodBodyTypeResolver = MethodBodyTypeResolver(body, binding, signature.returnType)

                methodBodyTypeResolver.resolve(environment, localContext)
            }

//              context.bind(funcType.toString(OrbitMangler), funcType)

            return signature
        } finally {
            // Garbage collect method parameter types
            localContext.removeAll(parameterBindings)
        }
    }
}