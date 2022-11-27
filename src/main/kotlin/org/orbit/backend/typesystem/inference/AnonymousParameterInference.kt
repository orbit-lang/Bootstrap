package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.AnyArrow
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.AnonymousParameterNode
import org.orbit.util.Invocation

// TODO - This is specific to projected signatures. Need a more general solution for arbitrary lambda values
object AnonymousParameterInference : ITypeInference<AnonymousParameterNode, ITypeEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    private fun inferProjectedSignature(node: AnonymousParameterNode, env: ProjectedSignatureEnvironment): AnyType {
        val indexedType = env.projectedSignature.parameters.elementAtOrNull(node.index)
            ?: throw invocation.make<TypeSystem>("Required Method `${env.projectedSignature}` of Trait `${env.parent.projection.target}` declares ${env.projectedSignature.parameters.count()} parameters but attempted to delegate by Anonymous Parameter at index ${node.index}", node)

        if (!TypeUtils.checkEq(env, indexedType, env.projectedSignature.returns)) {
            throw invocation.make<TypeSystem>("Anonymous Parameter \$${node.index} of Type `$indexedType` does not match return Type of projected Method `${env.projectedSignature}` of Trait `${env.parent.projection.target}`", node)
        }

        return env.projectedSignature.substitute(Substitution(env.parent.projection.target, env.parent.projection.source))
    }

    private fun inferLambdaParameter(node: AnonymousParameterNode, env: ISelfTypeEnvironment) : AnyType {
        val arrow = env.getSelfType() as AnyArrow
        val count = arrow.getDomain().count()

        if (count <= node.index) {
            throw invocation.make<TypeSystem>("Attempting to reference parameter at index ${node.index} in Lambda $arrow that only declares ${count} parameters", node)
        }

        return arrow.getDomain()[node.index]
    }

    override fun infer(node: AnonymousParameterNode, env: ITypeEnvironment): AnyType = when (env) {
        is ProjectedSignatureEnvironment -> inferProjectedSignature(node, env)
        is ISelfTypeEnvironment -> inferLambdaParameter(node, env)
        else -> TODO("UNSUPPORTED CONTEXT FOR ANONYMOUS PARAMETER INFERENCE: $env")
    }
}