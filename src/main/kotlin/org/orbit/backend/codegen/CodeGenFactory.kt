package org.orbit.backend.codegen

import org.orbit.backend.codegen.common.*
import org.orbit.core.nodes.*
import org.orbit.graph.components.StringKey
import java.math.BigInteger

interface CodeGenFactory<H: AbstractHeader> {
    fun getProgramUnit(node: ProgramNode, depth: Int) : AbstractProgramUnit<H>
    fun getModuleUnit(node: ModuleNode, depth: Int, header: H) : AbstractModuleUnit
    fun getMethodDefUnit(node: MethodDefNode, depth: Int) : AbstractMethodDefUnit = MethodDefUnit(node, depth)
    fun getMethodSignatureUnit(node: MethodSignatureNode, depth: Int) : AbstractMethodSignatureUnit
    fun getBlockUnit(node: BlockNode, depth: Int, stripBraces: Boolean, isMethodBody: Boolean) : AbstractBlockUnit = BlockUnit(node, depth, stripBraces, isMethodBody)
    fun getReturnStatementUnit(node: ReturnStatementNode, depth: Int, resultIsDeferred: Boolean, deferFunctions: List<StringKey>) : AbstractReturnStatementUnit
    fun getAssignmentStatementUnit(node: AssignmentStatementNode, depth: Int) : AbstractAssignmentStatementUnit
    fun getPrintStatementUnit(node: PrintNode, depth: Int) : AbstractPrintStatementUnit
    fun getDeferStatementUnit(node: DeferNode, depth: Int) : AbstractDeferStatementUnit
    fun getDeferCallUnit(node: DeferNode, depth: Int) : AbstractDeferCallUnit
    fun getMetaTypeUnit(node: MetaTypeNode, depth: Int, inFuncNamePosition: Boolean = false) : AbstractMetaTypeUnit = MetaTypeUnit(node, depth, inFuncNamePosition)
    fun getExpressionUnit(node: ExpressionNode, depth: Int) : AbstractExpressionUnit = ExpressionUnit(node, depth)
    fun getPropertyDefUnit(node: PairNode, depth: Int, isProtocol: Boolean = false) : AbstractPropertyDefUnit

    fun getIntLiteralUnit(node: LiteralNode<Pair<Int, BigInteger>>, depth: Int) : AbstractLiteralUnit<Pair<Int, BigInteger>>
    fun getSymbolLiteralUnit(node: SymbolLiteralNode, depth: Int) : AbstractLiteralUnit<Pair<Int, String>>
    fun getTypeLiteralUnit(node: LiteralNode<String>, depth: Int) : AbstractLiteralUnit<String>
    fun getCollectionLiteralUnit(node: CollectionLiteralNode, depth: Int) : AbstractCollectionLiteralUnit

    fun getUnaryExpressionUnit(node: UnaryExpressionNode, depth: Int) : AbstractUnaryExpressionUnit = UnaryExpressionUnit(node, depth)
    fun getBinaryExpressionUnit(node: BinaryExpressionNode, depth: Int) : AbstractBinaryExpressionUnit = BinaryExpressionUnit(node, depth)
    fun getIdentifierUnit(node: IdentifierNode, depth: Int) : AbstractIdentifierUnit = IdentifierUnit(node, depth)
    fun getRValueUnit(node: RValueNode, depth: Int) : AbstractRValueUnit = RValueUnit(node, depth)
    fun getConstructorUnit(node: ConstructorNode, depth: Int) : AbstractConstructorUnit
    fun getCallUnit(node: CallNode, depth: Int) : AbstractCallUnit
    fun getTypeExpressionUnit(node: TypeExpressionNode, depth: Int) : AbstractTypeExpressionUnit = TypeExpressionUnit(node, depth)

    fun getTypeDefUnit(node: TypeDefNode, depth: Int) : AbstractTypeDefUnit
    fun getTypeAliasUnit(node: TypeAliasNode, depth: Int) : AbstractTypeAliasUnit
    fun getTypeParameterUnit(node: TypeIdentifierNode, depth: Int) : AbstractTypeParameterUnit = TypeParameterUnit(node, depth)

    fun getProjectionWhereClauseUnit(node: WhereClauseNode, depth: Int) : AbstractProjectionWhereClauseUnit
    fun getPropertyProjectionUnit(node: AssignmentStatementNode, depth: Int) : AbstractPropertyProjectionUnit
}