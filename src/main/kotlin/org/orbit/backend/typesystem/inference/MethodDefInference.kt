package org.orbit.backend.typesystem.inference

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Decl
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.backend.typesystem.utils.TypeUtils
import org.orbit.core.nodes.MethodDefNode
import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.util.Invocation

object MethodDefInference : ITypeInference<MethodDefNode>, KoinComponent {
    private val invocation: Invocation by inject()

    @Suppress("NAME_SHADOWING")
    override fun infer(node: MethodDefNode, env: Env): AnyType {
        val env = when (val n = node.context) {
            null -> env
            else -> env + TypeSystemUtils.inferAs(n, env)
        }

        val signature = TypeSystemUtils.inferAs<MethodSignatureNode, IType.Signature>(node.signature, env, parametersOf(false))

        TypeSystemUtils.pushTypeAnnotation(signature.returns)

        val nEnv = env.extend(Decl.Clone())
            .withSelf(signature)

        if (node.signature.isInstanceMethod) {
            val decl = Decl.Assignment(node.signature.receiverIdentifier!!.identifier, signature.receiver)

            nEnv.extendInPlace(decl)
        }

        if (node.signature.parameterNodes.isNotEmpty()) {
            val pDecl = node.signature.getAllParameterPairs().map {
                Decl.Assignment(it.identifierNode.identifier, TypeSystemUtils.infer(it.typeExpressionNode, nEnv))
            }
            .reduce(Decl::plus)

            nEnv.extendInPlace(pDecl)
        }

        val returnType = TypeSystemUtils.infer(node.body, nEnv)

        return when (TypeUtils.checkEq(nEnv, returnType, signature.returns)) {
            true -> signature
            else -> throw invocation.make<TypeSystem>("Method `${node.signature.identifierNode.identifier}` declared return Type of `${signature.returns}`, found `${returnType}`", node)
        }
    }
}