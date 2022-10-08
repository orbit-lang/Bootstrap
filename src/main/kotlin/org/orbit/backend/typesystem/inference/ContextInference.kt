package org.orbit.backend.typesystem.inference

import org.koin.core.parameter.parametersOf
import org.orbit.backend.typesystem.components.*
import org.orbit.backend.typesystem.utils.TypeSystemUtils
import org.orbit.core.OrbitMangler
import org.orbit.core.getPath
import org.orbit.core.nodes.ContextNode
import org.orbit.core.nodes.EntityDefNode
import org.orbit.core.nodes.MethodDefNode
import org.orbit.core.nodes.MethodSignatureNode

object ContextInference : ITypeInference<ContextNode> {
    override fun infer(node: ContextNode, env: Env): AnyType {
        val envName = node.getPath().toString(OrbitMangler)
        val nEnv = env.extend(Decl.Clone(envName))
        val typeVars = mutableListOf<IType.TypeVar>()
        val values = mutableListOf<IRef>()

        node.typeVariables.forEach {
            val name = it.getPath().toString(OrbitMangler)

            nEnv.extendInPlace(Decl.TypeVariable(name))
            typeVars.add(IType.TypeVar(name))
        }

        val vDecls = node.variables.map {
            val type = TypeSystemUtils.infer(it, nEnv)
            val decl = Decl.Assignment(it.identifierNode.value, type)

            nEnv.extendInPlace(decl)
            values.add(Ref(it.identifierNode.value, type))

            decl
        }

        val mEnv = nEnv.withContext(Context(envName, typeVars, values))
        val entityDefs = node.body.filterIsInstance<EntityDefNode>()

        TypeSystemUtils.inferAll(entityDefs, mEnv)

        val signatureNodes = node.body.filterIsInstance<MethodDefNode>().map { it.signature }
        val signatures = TypeSystemUtils.inferAllAs<MethodSignatureNode, IType.Signature>(signatureNodes, mEnv, parametersOf(false))

        mEnv.extendAllInPlace(signatures.map { Decl.Signature(it) })

        TypeSystemUtils.inferAll(node.body, mEnv)

        vDecls.forEach {
            mEnv.reduceInPlace(it)
        }

        env.extendInPlace(Decl.Context(mEnv))

        return mEnv
    }
}