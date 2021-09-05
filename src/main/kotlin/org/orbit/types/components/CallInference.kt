package org.orbit.types.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.OrbitMangler
import org.orbit.core.nodes.CallNode
import org.orbit.graph.components.Annotations
import org.orbit.graph.extensions.annotate
import org.orbit.types.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.partial
import org.orbit.util.partialReverse
import org.orbit.util.pluralise

object CallInference : TypeInference<CallNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(context: Context, node: CallNode, typeAnnotation: TypeProtocol?): TypeProtocol {
        val receiverType = TypeInferenceUtil.infer(context, node.receiverExpression)
            as? Entity
            // TODO - Allow for signatures, potentially other types too
            ?: throw invocation.make<TypeSystem>("Only entity types may appear on the left-hand side of a call expression", node.receiverExpression)

        // TODO - There is way too much happening here.
        //  This should be simpler, or at least split up a bit
        if (node.isPropertyAccess) {
            val matches = receiverType.properties.filter { it.name == node.messageIdentifier.identifier }

            if (matches.isEmpty()) {
                throw invocation.make<TypeSystem>("Type '${receiverType.name}' has no property named '${node.messageIdentifier.identifier}'", node.messageIdentifier)
            } else if (matches.size > 1) {
                throw invocation.make<TypeSystem>("Type '${receiverType.name}' has multiple properties named '${node.messageIdentifier.identifier}'", node.messageIdentifier)
            }

            return matches.first().type
        } else {
            val receiverParameter = when (node.isInstanceCall) {
                true -> listOf(receiverType)
                else -> emptyList()
            }

            val parameterTypes = receiverParameter + node.parameterNodes.map(
                partialReverse(TypeInferenceUtil::infer, context)
            )

            val matches = mutableListOf<SignatureProtocol<*>>()
            for (binding in context.bindings.values) {
                if (binding.name != node.messageIdentifier.identifier) continue

                if (binding is InstanceSignature) {
                    val receiverSemantics = binding.receiver.type.equalitySemantics as AnyEquality

                    if (receiverSemantics.isSatisfied(context, binding.receiver.type, receiverType)) {
                        val all = binding.parameters
                            .map(Parameter::type)
                            .zip(parameterTypes)
                            .all {
                                val semantics = it.first.equalitySemantics as AnyEquality
                                semantics.isSatisfied(context, it.first, it.second)
                            }

                        if (all) {
                            matches.add(binding)
                        }
                    }
                } else if (binding is TypeSignature) {
                    val receiverSemantics = binding.receiver.equalitySemantics as AnyEquality

                    if (receiverSemantics.isSatisfied(context, binding.receiver, receiverType)) {
                        val all = binding.parameters
                            .map(Parameter::type)
                            .zip(parameterTypes)
                            .all {
                                val semantics = it.first.equalitySemantics as AnyEquality
                                semantics.isSatisfied(context, it.first, it.second)
                            }

                        if (all) {
                            matches.add(binding)
                        }
                    }
                }
            }

            if (matches.isEmpty()) {
                val params = if (parameterTypes.isEmpty()) "" else "(" + parameterTypes.joinToString(", ") { it.name } + ")"
                throw invocation.make<TypeSystem>(
                    "Receiver type '${receiverType.name}' does not respond to message '${node.messageIdentifier.identifier}' with parameter types $params",
                    node.messageIdentifier
                )
            } else if (matches.size > 1) {
                // TODO - Introduce some syntactic construct to manually allow differentiation in cases
                //  where 2 or more methods exist with the same name, same receiver & same parameters, but differ in the return type
                val candidates = matches.joinToString(
                    "\n\t\t",
                    transform = partial(SignatureProtocol<*>::toString, OrbitMangler)
                )

                throw invocation.make<TypeSystem>(
                    "Ambiguous method call '${node.messageIdentifier.identifier}' on receiver type '${receiverType.name}'. Found multiple candidates: \n\t\t${candidates}",
                    node.messageIdentifier
                )
            }

            val signature = matches.first()
            val expectedParameterCount = signature.parameters.size

            if (expectedParameterCount != parameterTypes.size) {
                throw invocation.make<TypeSystem>("Method '${signature.name}' expects $expectedParameterCount ${"parameter".pluralise(expectedParameterCount)}, found ${parameterTypes.size}", node)
            }

            signature.parameters.zip(parameterTypes)
                .forEachIndexed { idx, item ->
                    if (!(item.first.equalitySemantics as AnyEquality).isSatisfied(context, item.first.type, item.second)) {
                        throw invocation.make<TypeSystem>("Method '${signature.name}' expects parameter of type ${item.first.type.name} at index $idx, found ${item.second.name}", node)
                    }
                }

            node.annotate(signature, Annotations.Type)

            return receiverType
        }
    }
}