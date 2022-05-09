package org.orbit.backend.phase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.*
import org.orbit.core.components.CompilationSchemeEntry
import org.orbit.core.phase.ReifiedPhase
import org.orbit.frontend.phase.Parser
import org.orbit.types.next.components.Signature
import org.orbit.types.next.inference.InferenceUtil
import org.orbit.types.next.intrinsics.Native
import org.orbit.util.Invocation

data class Main(val mainSignature: Signature?) {
    companion object {
        val empty = Main(null)
    }
}

object MainResolver : ReifiedPhase<Parser.Result, Main>, KoinComponent {
    override val inputType: Class<Parser.Result> = Parser.Result::class.java
    override val outputType: Class<Main> = Main::class.java

    override val invocation: Invocation by inject()
    private val inferenceUtil: InferenceUtil by inject()

    private val mainPath = OrbitMangler.unmangle("Orb::Core::Main::Main::main::Orb::Core::Main::Main::Orb::Types::Intrinsics::Unit")

    override fun execute(input: Parser.Result) : Main {
        val mainFunc = inferenceUtil.getType(mainPath.toString(OrbitMangler)) as? Signature

        var result: Main = Main.empty

        // TODO - If a module is marked as main but not matching method is found, this should be an error
        if (mainFunc != null && mainFunc.receiver == Native.Types.Main.type && mainFunc.returns == Native.Types.Unit.type) {
            result = Main(mainFunc)
        }

        invocation.storeResult(CompilationSchemeEntry.mainResolver, result)

        return result
    }
}