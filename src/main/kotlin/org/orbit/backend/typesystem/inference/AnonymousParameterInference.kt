package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ProjectedSignatureEnvironment
import org.orbit.backend.typesystem.components.Substitution
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.AnonymousParameterNode
import org.orbit.util.Invocation

// TODO - This is specific to projected signatures. Need a more general solution for arbitrary lambda values
object AnonymousParameterInference : ITypeInference<AnonymousParameterNode, ProjectedSignatureEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: AnonymousParameterNode, env: ProjectedSignatureEnvironment): AnyType {
        val indexedType = env.projectedSignature.parameters.elementAtOrNull(node.index)
            ?: throw invocation.make<TypeSystem>("Required Method `${env.projectedSignature}` of Trait `${env.parent.projection.target}` declares ${env.projectedSignature.parameters.count()} parameters but attempted to delegate by Anonymous Parameter at index ${node.index}", node)

        if (!TypeUtils.checkEq(env, indexedType, env.projectedSignature.returns)) {
            throw invocation.make<TypeSystem>("Anonymous Parameter \$${node.index} of Type `$indexedType` does not match return Type of projected Method `${env.projectedSignature}` of Trait `${env.parent.projection.target}`", node)
        }

        return env.projectedSignature.substitute(Substitution(env.parent.projection.target, env.parent.projection.source))
    }
}