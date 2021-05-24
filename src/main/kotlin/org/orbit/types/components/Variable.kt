package org.orbit.types.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.SourcePosition
import org.orbit.types.phase.TypeChecker
import org.orbit.util.Invocation

data class Variable(val name: String) : Expression {
    private companion object : KoinComponent {
        // NOTE - We can defined this "statically" to save every instance of Variable having to do dependency resolution
        private val invocation: Invocation by inject()
    }

    override fun infer(context: Context, typeAnnotation: TypeProtocol?) : TypeProtocol {
        return context.get(name)
            // TODO - How can we spit out better error messages here?
            ?: throw invocation.make<TypeChecker>("Undefined variable '${name}'", SourcePosition.unknown)
    }
}