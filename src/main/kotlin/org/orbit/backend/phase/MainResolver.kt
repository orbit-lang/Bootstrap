package org.orbit.backend.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.phase.ReifiedPhase
import org.orbit.frontend.phase.Parser
import org.orbit.types.components.*
import org.orbit.util.Invocation

data class Main(val mainSignature: TypeSignature?) {
    companion object {
        val empty = Main(null)
    }
}

object MainResolver : ReifiedPhase<Parser.Result, Main>, KoinComponent {
    override val inputType: Class<Parser.Result> = Parser.Result::class.java
    override val outputType: Class<Main> = Main::class.java

    override val invocation: Invocation by inject()
    private val context: Context by injectResult(CompilationSchemeEntry.typeSystem)

    private val mainPath = Path("Orb", "Core", "Main", "Main", "main", "Orb", "Types", "Intrinsics", "Unit")

    override fun execute(input: Parser.Result) : Main {
        val mainFunc = context.get(mainPath.toString(OrbitMangler))
            as? TypeSignature

        var result: Main = Main.empty

        // TODO - If a module is marked as main but not matching method is found, this should be an error
        if (mainFunc != null && mainFunc.receiver == IntrinsicTypes.Main.type && mainFunc.returnType == IntrinsicTypes.Unit.type) {
            result = Main(mainFunc)
        }

        invocation.storeResult(CompilationSchemeEntry.mainResolver, result)

        return result
    }
}