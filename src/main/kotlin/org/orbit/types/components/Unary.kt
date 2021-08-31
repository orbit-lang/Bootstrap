package org.orbit.types.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.SourcePosition
import org.orbit.types.phase.TypeSystem
import org.orbit.util.Invocation

data class Unary(val op: String, val operand: TypeProtocol) : Expression, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(context: Context, typeAnnotation: TypeProtocol?): TypeProtocol {
        var matches = context.types
            .filterIsInstance<PrefixOperator>()
            .filter { it.operandType == operand }

        if (matches.isEmpty()) {
            throw invocation.make<TypeSystem>("Cannot find binary operator matching signature '$op${operand.name}'",
                SourcePosition.unknown
            )
        }

        if (matches.size > 1) {
            matches = matches.filter { it.symbol == op }

            if (matches.size == 1) {
                val resultType = matches.first().resultType

                if (typeAnnotation != null) {
                    val equalitySemantics = typeAnnotation.equalitySemantics as AnyEquality

                    if (!equalitySemantics.isSatisfied(context, typeAnnotation, resultType)) {
                        throw invocation.make<TypeSystem>("Type '${resultType.name} is not equal to type '${typeAnnotation.name}' using equality semantics '${equalitySemantics}",
                            SourcePosition.unknown
                        )
                    }
                }

                return resultType
            }
        }

        throw invocation.make<TypeSystem>("Failed to infer type of unary expression: '$op${operand.name}'",
            SourcePosition.unknown
        )
    }
}