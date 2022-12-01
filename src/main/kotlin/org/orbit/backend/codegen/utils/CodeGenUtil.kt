package org.orbit.backend.codegen.utils

import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatformTools
import org.orbit.backend.codegen.ICodeGenerator
import org.orbit.backend.codegen.ICodeGeneratorContext
import org.orbit.backend.codegen.manglers.SwiftMangler
import org.orbit.backend.codegen.swift.RootGeneratorContext
import org.orbit.core.INameMangler
import org.orbit.core.Path
import org.orbit.core.nodes.INode

interface ICodeGenTarget {
    fun getTargetName() : String
    fun getNameMangler() : INameMangler
}

enum class IntrinsicCodeGenTarget: ICodeGenTarget {
    Swift;

    override fun getTargetName(): String = name
    override fun getNameMangler(): INameMangler = SwiftMangler
}

class CodeGenUtil(val target: ICodeGenTarget) {
    var context: ICodeGeneratorContext = RootGeneratorContext

    inline fun <reified N: INode, reified G: ICodeGenerator<N>> generate(node: N) : String {
        val codeGenerator = KoinPlatformTools.defaultContext().get().get<G>(named("codeGen${target.getTargetName()}${node::class.java.simpleName}"))
        val result = codeGenerator.generate(node, context)

        context = result.context

        return result.source
    }

    inline fun <reified N: INode, reified G: ICodeGenerator<N>> generateAll(nodes: List<N>) : List<String>
        = nodes.map { generate<N, G>(it) }

    fun mangle(path: Path) : String
        = target.getNameMangler().mangle(path)
}