package org.orbit.backend

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.*
import org.orbit.frontend.phase.Parser
import org.orbit.types.components.Context
import org.orbit.types.components.InstanceSignature
import org.orbit.types.components.IntrinsicTypes
import org.orbit.types.components.Parameter
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
        val mainSignature = InstanceSignature("main", Parameter("", mainType.type), listOf(Parameter("", mainType.type)), IntrinsicTypes.Unit.type)
        val mainFunc = context.get(mainSignature.toString(OrbitMangler))
            as? InstanceSignature

        var result: Main = Main.empty

        // TODO - If a module is marked as main but not matching method is found, this should be an error
        if (mainFunc != null && mainFunc.receiver.type == mainType.type && mainFunc.returnType == IntrinsicTypes.Unit.type) {
            result = Main(mainFunc)
        }

        invocation.storeResult(CompilationSchemeEntry.mainResolver, result)

        return result
    }
}