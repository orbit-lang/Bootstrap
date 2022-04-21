package org.orbit.backend.codegen.c.units

import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.common.*
import org.orbit.core.nodes.*
import org.orbit.core.StringKey
import java.math.BigInteger

object CCodeGenFactory : CodeGenFactory<CHeader> {
    override fun getProgramUnit(node: ProgramNode, depth: Int): AbstractProgramUnit<CHeader>
        = ProgramUnit(node, depth)

    override fun getCollectionLiteralUnit(node: CollectionLiteralNode, depth: Int): AbstractCollectionLiteralUnit {
        TODO("Not yet implemented")
    }

    override fun getModuleUnit(node: ModuleNode, depth: Int, header: CHeader): AbstractModuleUnit
        = ModuleUnit(node, depth, header)

    override fun getMethodSignatureUnit(node: MethodSignatureNode, depth: Int): AbstractMethodSignatureUnit
        = MethodSignatureUnit(node, depth)

    override fun getReturnStatementUnit(node: ReturnStatementNode, depth: Int, resultIsDeferred: Boolean, deferFunctions: List<StringKey>): AbstractReturnStatementUnit
        = ReturnStatementUnit(node, depth, resultIsDeferred, deferFunctions)

    override fun getAssignmentStatementUnit(node: AssignmentStatementNode, depth: Int): AbstractAssignmentStatementUnit
        = AssignmentStatementUnit(node, depth)

    override fun getPrintStatementUnit(node: PrintNode, depth: Int): AbstractPrintStatementUnit
        = PrintStatementUnit(node, depth)

    override fun getDeferStatementUnit(node: DeferNode, depth: Int): AbstractDeferStatementUnit
        = DeferStatementUnit(node, depth)

    override fun getDeferCallUnit(node: DeferNode, depth: Int): AbstractDeferCallUnit
        = DeferCallUnit(node, depth)

    override fun getPropertyDefUnit(node: PairNode, depth: Int, isProtocol: Boolean): AbstractPropertyDefUnit
        = PropertyDefUnit(node, depth)

    override fun getIntLiteralUnit(node: LiteralNode<Pair<Int, BigInteger>>, depth: Int): AbstractLiteralUnit<Pair<Int, BigInteger>>
        = IntLiteralUnit(node, depth)

    override fun getSymbolLiteralUnit(node: SymbolLiteralNode, depth: Int): AbstractLiteralUnit<Pair<Int, String>>
        = SymbolLiteralUnit(node, depth)

    override fun getTypeLiteralUnit(node: LiteralNode<String>, depth: Int): AbstractLiteralUnit<String>
        = TypeLiteralUnit(node, depth)

    override fun getConstructorUnit(node: ConstructorNode, depth: Int): AbstractConstructorUnit
        = ConstructorUnit(node, depth)

    override fun getTypeDefUnit(node: TypeDefNode, depth: Int): AbstractTypeDefUnit
        = TypeDefUnit(node, depth)

    override fun getTypeAliasUnit(node: TypeAliasNode, depth: Int): AbstractTypeAliasUnit
        = TypeAliasUnit(node, depth)

    override fun getProjectionWhereClauseUnit(node: WhereClauseNode, depth: Int): AbstractProjectionWhereClauseUnit
        = ProjectionWhereClauseUnit(node, depth)

    override fun getPropertyProjectionUnit(node: AssignmentStatementNode, depth: Int): AbstractPropertyProjectionUnit
        = PropertyProjectionUnit(node, depth)

    override fun getLambdaLiteralUnit(node: LambdaLiteralNode, depth: Int): AbstractLambdaLiteralUnit {
        TODO("Not yet implemented")
    }

    override fun getCallUnit(node: InvokableNode, depth: Int): AbstractCallUnit<*> {
        TODO("Not yet implemented")
    }
}