package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.ProjectedSignatureEnvironment
import org.orbit.backend.typesystem.components.ProjectionEnvironment
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.AnyArrow
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.core.nodes.IDelegateNode
import org.orbit.core.nodes.MethodDelegateNode
import org.orbit.util.Invocation

object MethodDelegateInference : ITypeInference<MethodDelegateNode, ProjectionEnvironment>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun infer(node: MethodDelegateNode, env: ProjectionEnvironment): AnyType {
        val projectedSignature = env.projection.target.signatures.firstOrNull { it.name == node.methodName.identifier }
            ?: throw invocation.make<TypeSystem>("Trait `${env.projection.target}` does not declare required Signature `${node.methodName.identifier}`", node.methodName)

        val nEnv = ProjectedSignatureEnvironment(env, projectedSignature)

        return TypeInferenceUtils.inferAs<IDelegateNode, AnyArrow>(node.delegate, nEnv)
    }
}