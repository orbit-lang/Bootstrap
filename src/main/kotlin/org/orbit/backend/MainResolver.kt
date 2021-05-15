package org.orbit.backend

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.*
import org.orbit.frontend.Parser
import org.orbit.types.Context
import org.orbit.types.InstanceSignature
import org.orbit.types.IntrinsicTypes
import org.orbit.util.Invocation

data class Main(val mainSignature: InstanceSignature?) {
    companion object {
        val empty = Main(null)
    }
}

object MainResolver : ReifiedPhase<Parser.Result, Main>, KoinComponent {
    override val inputType: Class<Parser.Result> = Parser.Result::class.java
    override val outputType: Class<Main> = Main::class.java

    override val invocation: Invocation by inject()
    private val context: Context by injectResult(CompilationSchemeEntry.typeChecker)

    override fun execute(input: Parser.Result) : Main {
        val mainType = IntrinsicTypes.Main
        val mainFunc = context.get("main")
            as? InstanceSignature

        var result: Main = Main.empty

        if (mainFunc != null && mainFunc.receiver.type == mainType.type && mainFunc.returnType == IntrinsicTypes.Unit.type) {
            result = Main(mainFunc)
        }

        invocation.storeResult(CompilationSchemeEntry.mainResolver, result)

        return result
    }
}