package org.orbit.backend.typesystem.inference

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.ITypeEnvironment
import org.orbit.backend.typesystem.utils.TypeInferenceUtils
import org.orbit.backend.typesystem.utils.TypeSystemUtilsOLD
import org.orbit.core.nodes.BlockNode

object BlockInference : ITypeInference<BlockNode, ITypeEnvironment> {
    override fun infer(node: BlockNode, env: ITypeEnvironment): AnyType
        = TypeInferenceUtils.inferAll(node.body, env).lastOrNull() ?: IType.Unit
}

object BlockInferenceOLD : ITypeInferenceOLD<BlockNode> {
    override fun infer(node: BlockNode, env: Env): AnyType
        = TypeSystemUtilsOLD.inferAll(node.body, env).lastOrNull() ?: IType.Unit
}