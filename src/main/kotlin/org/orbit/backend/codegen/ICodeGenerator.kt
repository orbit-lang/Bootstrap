package org.orbit.backend.codegen

import org.orbit.core.nodes.INode

interface ICodeGeneratorContext {
    val depth: Int
}

data class GenericGeneratorContext(override val depth: Int, val typeParameters: List<String>) : ICodeGeneratorContext

fun ICodeGeneratorContext.indent() : String
    = "\t".repeat(depth)

data class AnyCodeGeneratorContext(override val depth: Int) : ICodeGeneratorContext

fun ICodeGeneratorContext.next() : ICodeGeneratorContext
    = AnyCodeGeneratorContext(depth + 1)

data class CodeGeneratorResult(val context: ICodeGeneratorContext, val source: String)

interface ICodeGenerator<N: INode> {
    fun generate(node: N, context: ICodeGeneratorContext) : CodeGeneratorResult
}

operator fun ICodeGeneratorContext.plus(source: String) : CodeGeneratorResult
    = CodeGeneratorResult(this, source)