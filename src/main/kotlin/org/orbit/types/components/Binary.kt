package org.orbit.types.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.SourcePosition
import org.orbit.types.phase.TypeChecker
import org.orbit.util.Invocation

data class Binary(val op: String, val left: TypeProtocol, val right: TypeProtocol) : Expression, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(context: Context, typeAnnotation: TypeProtocol?) : TypeProtocol {
        var matches = context.types
            .filterIsInstance<InfixOperator>()
            .filter { it.leftType == left && it.rightType == right }

        if (matches.isEmpty()) {
            // TODO - Source position should be retained as Type metadata
            throw invocation.make<TypeChecker>("Cannot find binary operator matching signature '${left.name} $op ${right.name}'",
                SourcePosition.unknown
            )
        }

        if (matches.size > 1) {
            // We have multiple signatures matching on function parameter types.
            // We need to refine the search by using the type annotation as the expected return type

            // TODO - We need to account for variance here?
            matches = matches.filter { it.symbol == op }

            if (matches.size == 1) {
                // We have a winner!
                // NOTE - We can't just return typeAnnotation here because that would effectively
                // erase the concrete operator return type if it is ±variant on typeAnnotation.
                // e.g. typeAnnotation is Number and 1 + 1 returns Int (which conforms to Number)
                val resultType = matches.first().resultType

                if (typeAnnotation != null) {
                    val equalitySemantics = typeAnnotation.equalitySemantics as AnyEquality

                    if (!equalitySemantics.isSatisfied(context, typeAnnotation, resultType)) {
                        throw invocation.make<TypeChecker>("Type '${resultType.name} is not equal to type '${typeAnnotation.name}' using equality semantics '${equalitySemantics}",
                            SourcePosition.unknown
                        )
                    }
                }

                return resultType
            }
        }

        throw invocation.make<TypeChecker>("Failed to infer type of binary expression: '${left.name} $op ${right.name}'",
            SourcePosition.unknown
        )
    }
}