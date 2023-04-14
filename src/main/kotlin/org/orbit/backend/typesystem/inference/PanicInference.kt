package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.components.Lazy
import org.orbit.backend.typesystem.components.Never
import org.orbit.backend.typesystem.intrinsics.OrbCoreErrors
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.PanicNode
import org.orbit.util.Invocation

object PanicInference : ITypeInference<PanicNode, ITypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: PanicNode, env: ITypeEnvironment): AnyType {
        val expr = TypeInferenceUtils.infer(node.expr, env)

        if (!OrbCoreErrors.errorTrait.isImplementedBy(expr, env)) {
            throw invocation.make<TypeSystem>("Panic expression must conform to Trait ${OrbCoreErrors.errorTrait}", node.expr)
        }

        return Lazy("!") { Never("Panic @ ${node.firstToken.position}") }
    }
}