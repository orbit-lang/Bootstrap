package org.orbit.types

import org.junit.jupiter.api.Test
import org.orbit.types.Type.Companion.IntType
import org.orbit.types.util.*
import kotlin.test.assertEquals

//class NextTypesTests : TestCase() {
////    @Test
////    fun testBox() {
////        val x = TypeLike.construct("x")
////        val y = TypeLike.construct("y")
////
////        val boxX = Box.fold(x)
////        val boxY = Box.fold(y)
////
////        assertEquals("x", x.name)
////        assertEquals("y", y.name)
////
////        assertEquals("x", boxX.name)
////        assertEquals("y", boxY.name)
////
////        val boxBoxX = Box.fold(boxX)
////
////        assertEquals("x", boxBoxX.name)
////
////        assertEquals("x", boxX.unfold(Never).name)
////        assertEquals("y", boxY.unfold(Never).name)
////        assertEquals("x", boxBoxX.unfold(Never).name)
////
////        assertEquals("x", Box.fold(Box.fold(Box.fold(Box.fold(x)))).unfold(Never).name)
////
////        assertEquals(Never, Never.unfold(Never))
////        assertEquals("_", Box.fold(Never).name)
////
////        val typeConstructorXY = Box.fold(x, y)
////
////        assertEquals("y[x]", typeConstructorXY.name)
////        assertEquals("x", typeConstructorXY.unfold(y).name)
////
////        assertEquals(Never, Value.constructor("a", Never))
////
////        val termX = Value.constructor("a", x)
////
////        assertEquals("(a : x)", termX.name)
////
////        val boxTermX = Box.fold(termX)
////        val boxTermXX = Box.fold(boxTermX, x)
////        val boxXTermX = Box.fold(x, boxTermX)
////
////        assertEquals("(a : x)", boxTermX.name)
////        assertEquals("x[(a : x)]", boxTermXX.name)
////        assertEquals("(a : x)[x]", boxXTermX.name)
////
////        assertEquals(Never, Value.constructor("a", typeConstructorXY))
////
////        assertEquals(Never, boxX.unfold(y))
////        assertEquals(Never, boxBoxX.unfold(y))
////
////        val funcXY = Func.construct(x, y)
////
////        assertEquals("x -> y", funcXY.name)
////
////        val funcBoxXBoxY = Func.construct(typeConstructorXY, y)
////
////        assertEquals("y[x] -> y", funcBoxXBoxY.name)
////
////        val funcTermXX = Func.construct(termX, x)
////
////        assertEquals("(a : x) -> x", funcTermXX.name)
////
////        val funcCurried = Func.construct(funcXY, funcXY)
////
////        assertEquals("x -> y -> x -> y", funcCurried.name)
////
////        val choiceXY = Choice.construct(x, y)
////
////        assertEquals("x | y", choiceXY.name)
////        assertEquals(Never, choiceXY.unfold(x))
////
////        assertEquals("x", choiceXY.unfold(Bool.True).name)
////        assertEquals("y", choiceXY.unfold(Bool.False).name)
////
////        val signatureBoolChoiceXY = Signature.construct(Bool, choiceXY)
////
////        assertEquals("{true false} -> x | y", signatureBoolChoiceXY.name)
////
////        val signatureBoolChoiceXYFuncs = signatureBoolChoiceXY.instantiate()
////
////        assertEquals(2, signatureBoolChoiceXYFuncs.count())
////        assertEquals("true -> x | y", signatureBoolChoiceXYFuncs[0].name)
////        assertEquals("false -> x | y", signatureBoolChoiceXYFuncs[1].name)
////
////        assertEquals("x", signatureBoolChoiceXY.unfold(Bool.True).name)
////        assertEquals("y", signatureBoolChoiceXY.unfold(Bool.False).name)
////
////        val universe = Universe(VarExpr.construct("x") to x)
////        val varX = VarExpr.construct("x")
////        val typeVarX = varX.evaluate(universe)
////
////        assertEquals("x", typeVarX.name)
////
////        val annotatedVarX = AnnotatedExpr.construct(varX, x)
////
////        assertEquals("x", annotatedVarX.evaluate(universe).name)
////
////        val id = Abstraction.construct(VarExpr.construct("a"), VarExpr.construct("a"))
////
////        assertEquals("(a => a)", id.name)
////
////        val appIdA = Apply.construct(id, VarExpr.construct("a"))
////
////        // Never because "a" is not bound yet
////        assertEquals(Never, appIdA.evaluate(universe))
////
////        val appIdX = Apply.construct(id, VarExpr.construct("x"))
////
////        assertEquals("x", appIdX.evaluate(universe).name)
////
////        val typeId = Abstraction.construct(AnnotatedVarExpr.construct("i", Bool), VarExpr.construct("i"))
////
////        val inferTypeID = Infer.construct(typeId)
////
//////        println(inferTypeID.name)
//////
//////        println(inferTypeID.evaluate(universe).name)
////    }
//
//    @Test
//    fun testProgram() {
//        val t = VarExpr.construct("t")
//        val f = VarExpr.construct("f")
//
//        val tt = TypeLike.construct("True")
//        val ft = TypeLike.construct("False")
//
//        val b = SumType.construct(tt, ft)
//        val tup = Tuple.construct(t, f)
//
//        val x = VarExpr.construct("x")
//        val fn = VarExpr.construct("fn")
//
//        val idSig = Abstraction.construct(listOf(VarExpr.construct("x")), VarExpr.construct("x"))
//        val id = VarExpr.construct("id")
//
//        val intrinsics = Universe(
//            types = mapOf(
//                "True" to AnyType,
//                "False" to AnyType,
//                "Bool" to b
//            ),
//            terms = mapOf(
//                t.name to tt,
//                f.name to ft,
//                id.name to idSig
//            )
//        )
//
//        val boxt = Box.construct(t, t)
//
//        val programScope = intrinsics.withTerms(boxt.name to boxt.infer(intrinsics))
//
//        val program = Program.construct(programScope,
//            Abstraction.construct(
//                listOf(x),
//                Abstraction.construct(
//                    listOf(fn),
//                    Apply.construct(fn, listOf(x))
//                )
//            )
//        )
//
//        println(program.name)
//        println(program.evaluate(Universe.empty).name)
//    }
//}

/*******************************************************************/

fun Int.toSuperscript() : String = when (this) {
    0 -> "⁰"
    1 -> "¹"
    2 -> "²"
    3 -> "\u2073"
    4 -> "\u2074"
    5 -> "\u2075"
    6 -> "\u2076"
    7 -> "\u2077"
    8 -> "\u2078"
    9 -> "\u2079"
    else -> "ⁿ"
}

//interface Obj {
//    val name: String
//}
//
//interface Type : Obj {
//    val typeIdentifier: String
//    val ctx: Ctx
//
//    override val name: String
//        get() = "${typeIdentifier}[${ctx.shortName}]"
//}
//
//object Never : Type {
//    override val typeIdentifier: String = "NEVER"
//    override val ctx: Ctx = Ctx.𝜞
//}
//
//interface Term : Obj
//
//interface TypedTerm : Term {
//    val type: Type
//}
//
//fun Map.Entry<V, Obj>.pretty() : String = when (value) {
//    is TypedTerm -> "${key.name}: ${value.name} ⊢ ${(value as TypedTerm).type.name}"
//    else -> "${key.name}: ${value.name}"
//}
//
//fun Map<V, Obj>.pretty() : String
//    = map(Map.Entry<V, Obj>::pretty).joinToString(", ")
//
//class Ctx(val level: Int = 0, private var bindings: Map<V, Obj> = emptyMap()) : Term {
//    companion object {
//        val 𝜞 = Ctx(0)
//    }
//
//    override val name: String
//        get() = "$shortName(${bindings.pretty()})"
//
//    val shortName: String
//        get() = "ψ${level.toSuperscript()}"
//
//    operator fun get(v: V) : Obj? {
//        if (v.level == level) {
//            return bindings[v]
//        }
//
//        return bindings.values
//            .filterIsInstance<Ctx>()
//            .firstOrNull { it.level == v.level }
//    }
//
//    fun merge(other: Ctx) : Ctx
//        = Ctx(level, bindings + other.bindings)
//
//    fun extend(newBinding: Pair<V, Obj>) {
//        bindings = when (newBinding.second) {
//            is Ctx -> {
//                val existingCtx = get(newBinding.first) as? Ctx
//
//                when (existingCtx) {
//                    null -> bindings + mapOf(newBinding)
//                    else -> bindings.filter { it.key != newBinding.first } +
//                        (newBinding.first to existingCtx.merge(newBinding.second as Ctx))
//                }
//            }
//
//            else -> bindings + mapOf(newBinding)
//        }
//    }
//
//    fun with(newBindings: Map<V, Obj> = emptyMap()) : Ctx
//        = Ctx(level, bindings + newBindings)
//
//    fun with(newBinding: Pair<V, Obj>) : Ctx
//        = Ctx(level, bindings + newBinding)
//}
//
//data class I(val value: Int) : TypedTerm {
//    override val name: String = "$value"
//    override val type: Type = IntType(Ctx.𝜞)
//}
//
//data class IntType(override val ctx: Ctx) : Type {
//    override val typeIdentifier: String
//        get() = "int"
//}
//
//data class V(val level: Int = 1, val symbol: String) : Term {
//    override val name: String = "$symbol${level.toSuperscript()}"
//}
//
//interface Redex<C, E, CT: Obj, ET: Obj> {
//    fun construct(data: C, ctx: Ctx) : CT
//    fun eliminate(data: E, ctx: Ctx) : ET
//}
//
//object VarRedex : Redex<VarRedex.CData, V, V, Obj> {
//    data class CData(val v: V, val term: Term)
//
//    fun construct(level: Int = 0, symbol: String, term: Term, ctx: Ctx)
//        = construct(CData(V(level, symbol), term), ctx)
//
//    override fun construct(data: CData, ctx: Ctx): V {
//        ctx.extend(data.v to data.term)
//        return data.v
//    }
//
//    override fun eliminate(data: V, ctx: Ctx): Obj = ctx[data]!!
//}
//
//data class Lambda(val level: Int = 1, val parameterName: String, val body: Term) : Term {
//    val parameter = V(level, parameterName)
//
//    override val name: String
//        get() = "λ${parameter.name}.${body.name}"
//}
//
//object LambdaRedex : Redex<LambdaRedex.CData, LambdaRedex.EData, Lambda, Obj> {
//    data class CData(val p: String, val body: Term)
//    data class EData(val lam: Term, val argument: Term)
//
//    override fun construct(data: CData, ctx: Ctx): Lambda {
//        return Lambda(ctx.level, data.p, data.body)
//    }
//
//    override fun eliminate(data: EData, ctx: Ctx): Obj {
//        val fn: Lambda = when (data.lam) {
//            is Lambda -> data.lam
//            is V -> VarRedex.eliminate(data.lam, ctx) as Lambda
//            else -> throw Exception("Cannot apply ${data.lam.name}")
//        }
//
//        val nCtx = ctx.with(fn.parameter to data.argument)
//
////        println("NCTX: ${nCtx.name}")
//
//        return when (data.argument) {
//            is V -> VarRedex.eliminate(data.argument, nCtx)
//            is Lambda -> LambdaRedex.eliminate(EData(fn, data.argument), nCtx)
//            else -> TODO("???")
//        }
//    }
//}
//
////object BoxRedex : Redex<> {
////    data class CData()
////}
//
////object Let : Redex<Let.CData, Let.EData> {
////    data class CData(val v: Term.V, val expr: Obj)
////    data class EData(val body: Function1<Ctx, Obj>)
////
////    override fun construct(data: CData, ctx: Ctx): Obj
////        = Ctx(ctx.level + 1).with(data.v to data.expr)
////
////    override fun eliminate(data: EData, ctx: Ctx): Obj
////        = data.body(ctx)
////}
//
//fun <C, E, CT: Obj, ET: Obj, R: Redex<C, E, CT, ET>> eval(redex: R, elimData: E, ctx: Ctx) : String
//    = redex.eliminate(elimData, ctx).name
//
//class CtxTests : TestCase() {
//    @Test
//    fun testCtx() {
//        val 𝜞 = Ctx(1)
//        val 𝛙 = Ctx(2)
//
//        val x = V(1, "x")
//        val a = V(2, "a")
//        val φ = V(1, "φ")
//    }
//}

interface Obj {
    val name: String
}

fun List<Obj>.pretty() : String
    = map(Obj::name).joinToString(" ")

interface IType : Term<IType>

data class Type(override val name: String) : IType, Term<IType> {
    companion object {
        val AnyType = Type("*")
        val Never = Type("_")
        val IntType = Type("int")
        val BoolType = Type("bool")

        fun bool(b: Boolean) : Type = when (b) {
            true -> AnyType
            else -> Never
        }
    }

    override fun evaluate(ctx: Ctx): Type = this
}

data class NominalType(val tag: String) : IType, Term<IType> {
    override val name: String = "('${tag}')"

    override fun evaluate(ctx: Ctx): IType = this
}

data class TypeHole(val v: Var<IType>, val headType: Term<IType>) : IType, Term<IType> {
    override val name: String = "(${v.name}?:${headType.name})"

    override fun evaluate(ctx: Ctx): IType = this
}

interface Term<R: Obj> : Obj {
    fun evaluate(ctx: Ctx) : R
}

data class Var<O: Obj>(val symbol: String) : Term<O> {
    override val name: String = "$symbol"

    inline fun <reified O: Obj> objType() : Class<O> = O::class.java

    override fun evaluate(ctx: Ctx): O {
        return ctx.get(symbol) as? O
            ?: throw Exception("Undefined var $symbol")
    }
}

interface CompoundType : IType, Term<IType> {
    val head: Term<IType>
    val tail: List<Term<IType>>
}

data class Tuple(override val head: Term<IType>, override val tail: List<Term<IType>> = emptyList()) : CompoundType {
    override val name: String get() = when (tail.isEmpty()) {
        true -> "(${head.name})"
        else -> "(${head.name} ${tail.pretty()})"
    }

    override fun evaluate(ctx: Ctx): Tuple {
        val headType = head.evaluate(ctx)
        val tailTypes = tail.map { it.evaluate(ctx.with("self", headType)) }

        return Tuple(headType, tailTypes)
    }
}

data class TypeDecl(val typeName: Type, val params: List<Term<*>> = emptyList()) : Obj {
    override val name: String = "${typeName.name} ${params.pretty()}"
}

data class Decl(val typeDecl: TypeDecl, val expr: Tuple) : Term<Ctx> {
    override val name: String = "${typeDecl.name} : ${expr.name}"

    override fun evaluate(ctx: Ctx): Ctx
        = ctx.with(Var(typeDecl.typeName.name), expr)
}

fun <K: Obj> Map.Entry<Var<K>, Obj>.pretty() : String
    = "${key.name}:${value.name}"

fun <K: Obj> Map<Var<K>, Obj>.pretty() : String
    = entries.joinToString(" ", transform = Map.Entry<Var<K>, Obj>::pretty)

interface Value : Term<Value> {
    val type: IType

    override fun evaluate(ctx: Ctx): Value = this
}

data class IntValue(val value: Int) : Value {
    override val type: IType = Type.IntType
    override val name: String = "$value:${type.name}"
}

sealed class BoolValue(val value: Boolean) : Value {
    object True : BoolValue(true)
    object False : BoolValue(false)

    final override val type: IType = Type.BoolType
    override val name: String = "$value:${type.name}"
}

typealias TypeVar = Var<IType>
typealias TermVar = Var<Term<Value>>

@Suppress("UNCHECKED_CAST")
data class Ctx(val types: Map<TypeVar, IType> = emptyMap(), val terms: Map<TermVar, Term<Value>> = emptyMap()) : Obj {
    constructor(vararg types: String) : this(types.map { TypeVar(it) to NominalType(it) }.toMap())

    override val name: String
        get() = "ψ(${types.pretty()})"

    operator fun <O: Obj> get(v: Var<O>) : O? {
        val result = types[Var(v.symbol)]

        if (result != null) return result as? O

        return terms[Var(v.symbol)] as? O
    }

    operator fun get(v: TypeVar) : IType? = types[v]
    operator fun get(v: TermVar) : Value? = null
    operator fun <O: Obj> get(v: String) : O? = get(Var<O>(v))

    fun with(v: TypeVar, type: IType) : Ctx
        = Ctx(types + (v to type))

    fun with(v: String, type: IType) : Ctx
        = Ctx(types + (TypeVar(v) to type))

    fun with(v: TermVar, term: Term<Value>) : Ctx
        = Ctx(types, terms + (v to term))

    fun with(v: String, term: Term<Value>) : Ctx
        = Ctx(types, terms + (TermVar(v) to term))
}

data class TypeInstance(override val head: Term<IType>, override val tail: List<Term<IType>>) : CompoundType {
    override val name: String = "(${head.name} ${tail.pretty()})"

    override fun evaluate(ctx: Ctx): IType {
        val headTuple = head.evaluate(ctx) as? Tuple ?: return Type.Never
        val headType = headTuple.head.evaluate(ctx)
        val nCtx = ctx.with("self", headType)
        val tTailTypes = headTuple.tail.map { it.evaluate(nCtx) }
        val cTailTypes = tail.map { it.evaluate(ctx) }

        val tCount = tTailTypes.count()
        val cCount = cTailTypes.count()

        if (cCount > tCount) {
            return Type.Never
        } else if (cCount < tCount) {
            val tSlice = tTailTypes.subList(0, cCount)
            val tailChecks = tSlice.zip(cTailTypes)
                .map(::TypeCheck)
                .map { it.evaluate(ctx) }

            if (tailChecks.contains(Type.Never)) return Type.Never

            val tEnd = tTailTypes.subList(cCount, tCount)

            return Tuple(headType, cTailTypes + tEnd)
        } else {
            val tailChecks = tTailTypes.zip(cTailTypes)
                .map(::TypeCheck)
                .map { it.evaluate(ctx) }

            if (!tailChecks.contains(Type.Never)) {
                return Tuple(headType, cTailTypes)
            }
        }

        return Type.Never
    }
}

data class TypeCheck(val template: Term<out Obj>, val scrutinee: Term<out Obj>) : Term<Type> {
    constructor(pair: Pair<Term<IType>, Term<IType>>) : this(pair.first, pair.second)

    override val name: String = "(${template.name} ⟸ ${scrutinee.name})"

    private fun compoundCheck(ctx: Ctx, a: CompoundType, b: CompoundType) : Type {
        val headCheck = TypeCheck(a.head, b.head)
            .evaluate(ctx)

        if (headCheck == Type.Never) return Type.Never

        val tailA = a.tail
        val tailB = b.tail

        if (tailA.count() != tailB.count()) return Type.Never

        val checks = tailA.zip(tailB).map(::TypeCheck).map { it.evaluate(ctx) }

        if (checks.contains(Type.Never)) return Type.Never

        return Type.AnyType
    }

    override fun evaluate(ctx: Ctx): Type {
        val templateType = when (val t = template.evaluate(ctx)) {
            is Value -> t.type
            else -> t
        }

        val scrutineeType = when (val s = scrutinee.evaluate(ctx)) {
            is Value -> s.type
            else -> s
        }

        if (templateType == Type.Never || scrutineeType == Type.Never) return Type.Never
        if (templateType == Type.AnyType) return Type.AnyType

        val result = when (templateType) {
            is TypeInstance -> when (scrutineeType) {
                is TypeInstance -> compoundCheck(ctx, templateType, scrutineeType)
                else -> Type.Never
            }

            is TypeHole -> when (scrutineeType) {
                is IType -> TypeCheck(templateType.headType, scrutineeType).evaluate(ctx)
                else -> Type.Never
            }

            is NominalType -> when (scrutineeType) {
                is NominalType -> Type.bool(templateType.tag == scrutineeType.tag)
                else -> Type.Never
            }

            is Type -> when (scrutineeType) {
                is Type -> Type.bool(templateType.name == scrutineeType.name)
                is Tuple -> TypeCheck(templateType, scrutineeType.head).evaluate(ctx)
                else -> Type.Never
            }

            is Tuple -> when (scrutineeType) {
                is Tuple -> compoundCheck(ctx, templateType, scrutineeType)
                else -> Type.Never
            }

            else -> Type.Never
        }

        println("${templateType.name} ⟸ ${scrutineeType.name} :: ${result.name}")

        return result
    }
}

fun typeCheck(ctx: Ctx, a: Term<out Obj>, b: Term<out Obj>, expectation: Type) {
    val check = TypeCheck(a, b)
    val result = check.evaluate(ctx)
    println("-------TYPE CHECK ${a.name} ⟸ ${b.name}-------")
    println("${check.name} : ${result.name}")
    println("-----------------------------------------------")

    assertEquals(expectation, result)
}

fun typeCheckT(ctx: Ctx, a: Term<out Obj>, b: Term<out Obj>)
    = typeCheck(ctx, a, b, Type.AnyType)

fun typeCheckF(ctx: Ctx, a: Term<out Obj>, b: Term<out Obj>)
    = typeCheck(ctx, a, b, Type.Never)

class NextTypesTests {
    @Test
    fun test() {
        val int = Var<IType>("int")
        val bool = Var<IType>("bool")
        var ctx = Ctx("int", "bool")
        val tree = Var<IType>("tree")
        val a = Var<IType>("a")
        val aHole = TypeHole(a, Type.AnyType)
        val treeType = Tuple(Type.AnyType, listOf(aHole))

        ctx = ctx.with(tree, treeType)

        val leaf = Var<IType>("leaf")
        val leafType = Tuple(tree, listOf(aHole))

        ctx = ctx.with(leaf, leafType)

        val treeInt = TypeInstance(treeType, listOf(int))
        val treeBool = TypeInstance(treeType, listOf(bool))

        typeCheckT(ctx, int, int)
        typeCheckT(ctx, bool, bool)
        typeCheckF(ctx, int, bool)
        typeCheckF(ctx, bool, int)
        typeCheckT(ctx, tree, tree)
        typeCheckT(ctx, treeInt, treeInt)
        typeCheckT(ctx, treeBool, treeBool)
        typeCheckF(ctx, treeInt, treeBool)
        typeCheckF(ctx, treeBool, treeInt)
        typeCheckT(ctx, tree, treeInt)
        typeCheckF(ctx, treeInt, tree)

        val treeHole = TypeHole(Var("t"), tree)

        val branch = Var<IType>("branch")
        val branchType = Tuple(treeType, listOf(treeHole))
        val intBranch = TypeInstance(branchType, listOf(treeInt))

        ctx = ctx.with(branch, branchType)

        typeCheckT(ctx, tree, intBranch)

        val typeConstructorAB = Var<IType>("tcAB")
        val tcABType = Tuple(Type.AnyType, listOf(aHole, aHole))

        ctx = ctx.with(typeConstructorAB, tcABType)

        val tcABInt = TypeInstance(typeConstructorAB, listOf(int))
        val tcABIntInt = TypeInstance(tcABInt, listOf(int))
        val tcABBool = TypeInstance(typeConstructorAB, listOf(bool))
        val tcABBoolBool = TypeInstance(tcABBool, listOf(bool))

        typeCheckT(ctx, tcABType, tcABInt)
        typeCheckF(ctx, tcABInt, tcABType)

        typeCheckT(ctx, tcABType, tcABBool)
        typeCheckF(ctx, tcABBool, tcABType)

        typeCheckF(ctx, tcABInt, tcABBool)
        typeCheckF(ctx, tcABBool, tcABInt)

        typeCheckF(ctx, tcABIntInt, tcABBoolBool)
        typeCheckF(ctx, tcABBoolBool, tcABIntInt)

        typeCheckT(ctx, tcABInt, tcABIntInt)

        val z = TermVar("z")
        val s = TermVar("s")

        ctx = ctx.with(z, IntValue(0))
        ctx = ctx.with(s, IntValue(1))

        println(z.name)
        println(s.name)

        val zVal = z.evaluate(ctx)
        val sVal = s.evaluate(ctx)

        println(zVal.name)
        println(sVal.name)

        typeCheckT(ctx, z, IntType)
        typeCheckT(ctx, IntType, z)

        typeCheckT(ctx, zVal, IntType)
        typeCheckT(ctx, zVal, z)
    }
}