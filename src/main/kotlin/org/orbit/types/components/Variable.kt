package org.orbit.types.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.SourcePosition
import org.orbit.types.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer

data class Variable(val name: String) : Expression {
    private companion object : KoinComponent {
        // NOTE - We can defined this "statically" to save every instance of Variable having to do dependency resolution
        private val invocation: Invocation by inject()
        private val printer: Printer by inject()
    }

    override fun infer(context: Context, typeAnnotation: TypeProtocol?) : TypeProtocol {
        val type = context.get(name) ?: throw invocation.make<TypeSystem>("Undefined variable '${name}'", SourcePosition.unknown)
        if (typeAnnotation == null) return type

        val eq = typeAnnotation.equalitySemantics as AnyEquality

        if (!eq.isSatisfied(context, typeAnnotation, type)) {
            throw invocation.make<TypeSystem>("Expected variable '$name' to be of type ${typeAnnotation.toString(printer)}, found ${type.toString(printer)}", SourcePosition.unknown)
        }

        return type
    }
}