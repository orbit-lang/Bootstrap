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

interface IType : Term<IType> {
    override fun mirror(ctx: Ctx): IMirror<IType> = TypeMirror(this)
}

interface IMirror<O: Obj> : Obj {
    fun match(ctx: Ctx, obj: O) : Boolean
}

interface Mirrored<O: Obj> : Obj {
    fun mirror(ctx: Ctx) : IMirror<O>
}

data class TypeMirror(val source: IType) : IMirror<IType> {
    override val name: String = "(Mirror * ${source.name})"

    override fun match(ctx: Ctx, obj: IType): Boolean
        = TypeCheck(source, obj).evaluate(ctx) != Type.Never
}

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

interface Term<R: Obj> : Obj, Mirrored<R> {
    fun evaluate(ctx: Ctx) : R

    fun convert(conversion: Conversion) : Term<*> = when (conversion) {
        is AlphaConversion -> convert(conversion)
        is BetaConversion -> convert(conversion)
        else -> Type.Never
    }

    fun convert(conversion: AlphaConversion) : Term<*> = this
    fun convert(conversion: BetaConversion) : Term<*> = this

    fun convertAll(conversions: List<Conversion>) : Term<*> {
        var result = this
        for (reduction in conversions) {
            result = result.convert(reduction) as Term<R>
        }

        return result
    }

    fun freeVariables(ctx: Ctx) : List<String> = emptyList()
}

interface Normal<O: Obj> : Term<O>

data class VarMirror<O: Obj>(val source: Var<O>) : IMirror<O> {
    override val name: String = "(Mirror ${source.name})"

    override fun match(ctx: Ctx, obj: O): Boolean {
        val sourceValue = source.evaluate(ctx)

        return obj.name == sourceValue.name
    }
}

data class Var<O: Obj>(val symbol: String) : Term<O> {
    override val name: String = symbol

    inline fun <reified O: Obj> objType() : Class<O> = O::class.java

    override fun evaluate(ctx: Ctx): O {
        return ctx.get(symbol) as? O
            ?: throw Exception("Undefined var $symbol")
    }

    override fun convert(conversion: AlphaConversion): Term<*> = when (symbol) {
        conversion.sourceSymbol -> Var<O>("$symbol'")
        else -> this
    }

    override fun convert(conversion: BetaConversion): Term<*> = when (conversion.symbol) {
        symbol -> conversion.substitute
        else -> this
    }

    override fun mirror(ctx: Ctx): IMirror<O> = VarMirror(this)

    override fun freeVariables(ctx: Ctx): List<String> = when (ctx.containsTerm(symbol)) {
        true -> listOf(symbol)
        else -> emptyList()
    }
}

interface CompoundType : IType, Term<IType> {
    val head: Term<IType>
    val tail: List<Term<IType>>
}

data class Let<R: Obj, T: Term<*>>(val head: Var<*>, val expr: Term<T>, val body: Term<R>) : Term<R> {
    override val name: String = "(let ${head.name} = ${expr.name} in ${body.name})"

    override fun evaluate(ctx: Ctx): R {
        val exprValue = expr.evaluate(ctx)
        val nCtx = ctx.with(head.symbol, exprValue)

        return body.evaluate(nCtx)
    }

    override fun mirror(ctx: Ctx): IMirror<R> {
        TODO("Not yet implemented")
    }
}

data class TupleMirror(val source: Tuple) : IMirror<Tuple> {
    override val name: String = "(Mirror ${source.name})"

    override fun match(ctx: Ctx, obj: Tuple): Boolean {
        if (obj.tail.count() != source.tail.count()) return false

        val headMirror = source.head.mirror(ctx)
        val tailMirrors = source.tail.map { it.mirror(ctx) }
            .zip(obj.tail)

        val headValue = obj.head.evaluate(ctx)

        return headMirror.match(ctx, headValue) && tailMirrors.all {
            it.first.match(ctx, it.second.evaluate(ctx))
        }
    }
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

    override fun mirror(ctx: Ctx): IMirror<IType> = TupleMirror(this) as IMirror<IType>
}

data class Product(val left: Term<*>, val right: Term<*>) : Term<Product> {
    override val name: String = "(${left.name} + ${right.name})"

    override fun evaluate(ctx: Ctx): Product = this
    override fun mirror(ctx: Ctx): IMirror<Product> {
        TODO("Not yet implemented")
    }
}

data class TypeDecl(val typeName: Type, val params: List<Term<*>> = emptyList()) : Obj {
    override val name: String = "${typeName.name} ${params.pretty()}"
}

fun <K: Obj> Map.Entry<Var<K>, Obj>.pretty() : String
    = "${key.name}:${value.name}"

fun <K: Obj> Map<Var<K>, Obj>.pretty() : String
    = entries.joinToString(" ", transform = Map.Entry<Var<K>, Obj>::pretty)

interface Value : Normal<Value> {
    val type: IType

    override fun evaluate(ctx: Ctx): Value = this
}

data class ValueMirror<V: Value>(val value: V) : IMirror<V> {
    override val name: String = "(Mirror ${value.name})"

    override fun match(ctx: Ctx, obj: V): Boolean = obj == value
}

data class IntRangeMirror(val lowerBound: IntValue, val upperBound: IntValue, val inclusive: Boolean) : IMirror<IntValue> {
    override val name: String = "(Mirror ${lowerBound.name} ... ${upperBound.name})"

    override fun match(ctx: Ctx, obj: IntValue): Boolean = when (inclusive) {
        true -> obj.value >= lowerBound.value && obj.value <= upperBound.value
        else -> obj.value > lowerBound.value && obj.value < upperBound.value
    }
}

data class IntValue(val value: Int) : Value {
    override val type: IType = Type.IntType
    override val name: String = "$value:${type.name}"

    override fun mirror(ctx: Ctx): IMirror<Value>
        = IntRangeMirror(this, this, true) as IMirror<Value>

    override fun equals(other: Any?): Boolean = when (other) {
        is IntValue -> other.value == value
        else -> false
    }
}

data class BoolMirror(val value: BoolValue) : IMirror<BoolValue> {
    override val name: String = "(Mirror ${value.name})"

    override fun match(ctx: Ctx, obj: BoolValue): Boolean = when (obj) {
        value -> true
        else -> false
    }
}

sealed class BoolValue(val value: Boolean) : Value {
    object True : BoolValue(true)
    object False : BoolValue(false)

    final override val type: IType = Type.BoolType
    override val name: String = "$value:${type.name}"

    override fun mirror(ctx: Ctx): IMirror<Value>
        = BoolMirror(this) as IMirror<Value>

    override fun equals(other: Any?): Boolean = when (other) {
        is BoolValue -> other.value == value
        else -> false
    }
}

typealias TypeVar = Var<IType>
typealias TermVar = Var<Term<Value>>

data class Box<O: Obj>(val value: Term<O>) : Term<Box<O>> {
    override val name: String = "(Box ${value.name})"

    override fun evaluate(ctx: Ctx): Box<O> = this
    override fun mirror(ctx: Ctx): IMirror<Box<O>> {
        TODO("Not yet implemented")
    }
}

data class Unbox<O: Obj>(val box: Term<Box<O>>) : Term<O> {
    override val name: String = "(Unbox ${box.name})"

    override fun evaluate(ctx: Ctx): O = box.evaluate(ctx).value.evaluate(ctx)
    override fun mirror(ctx: Ctx): IMirror<O> {
        TODO("Not yet implemented")
    }
}

interface Conversion

data class AlphaConversion(val sourceSymbol: String) : Conversion
data class BetaConversion(val symbol: String, val substitute: Term<*>) : Conversion

data class Lambda(val params: List<Var<*>>, val body: Term<*>) : Normal<Lambda> {
    override val name: String = "(λ ${params.pretty()} ⟹ ${body.name})"

    override fun convert(conversion: AlphaConversion): Term<*> {
        val nParams = params.map { it.convert(conversion) as Var<*> }
        val nBody = body.convert(conversion)

        return Lambda(nParams, nBody)
    }

    override fun convert(conversion: BetaConversion): Term<*>
        = Lambda(params, body.convert(conversion))

    override fun evaluate(ctx: Ctx): Lambda = this
    override fun mirror(ctx: Ctx): IMirror<Lambda> {
        TODO("Not yet implemented")
    }

    override fun freeVariables(ctx: Ctx): List<String>
        = body.freeVariables(ctx)
}

interface EvaluationStrategy {
    fun evaluate(ctx: Ctx, term: Term<*>) : Term<*>
}

object CallByValue : EvaluationStrategy {
    override fun evaluate(ctx: Ctx, term: Term<*>): Term<*> = term.evaluate(ctx) as Term<*>
}

object CallByName : EvaluationStrategy {
    override fun evaluate(ctx: Ctx, term: Term<*>): Term<*> = term
}

object Missing : Normal<Missing> {
    override val name: String = "?"

    override fun evaluate(ctx: Ctx): Missing = this
    override fun mirror(ctx: Ctx): IMirror<Missing> {
        TODO("Not yet implemented")
    }
}

data class Application(val lambda: Term<Lambda>, val args: List<Term<*>>, val strategy: EvaluationStrategy = CallByValue) : Term<Obj> {
    override val name: String = "(${lambda.name} ${args.pretty()})"

    override fun convert(conversion: Conversion): Term<*>
        = Application(lambda.convert(conversion) as Term<Lambda>, args.map { it.convert(conversion) }, strategy)

    override fun freeVariables(ctx: Ctx): List<String>
        = args.flatMap { it.freeVariables(ctx) }

    override fun evaluate(ctx: Ctx): Obj {
        val fn = lambda.evaluate(ctx)
        val pCount = fn.params.count()
        val aCount = args.count()

        val args = if (aCount == pCount) fn.params.zip(args).map {
            when (it.second) {
                is Missing -> Lambda(listOf(it.first), it.first)
                else -> it.second
            }
        } else args

        val a = Application(fn, args)

        println("EXPANDED ARG HOLES: ${a.name}")

        val betaConversions = fn.params.zip(args).map {
            BetaConversion(it.first.symbol, it.second)
        }

        val bindings = fn.params.zip(args).map {
            Pair(it.first.symbol, it.second)
        }

        val nCtx = ctx.with(bindings)

        var nBody = fn.body.convertAll(betaConversions)

        val alphaConversions = nBody.freeVariables(nCtx).map {
            AlphaConversion(it)
        }

        nBody = nBody.convertAll(alphaConversions)

        if (aCount < pCount) {
            val remaining = fn.params.subList(aCount, pCount)

            return Lambda(remaining, nBody)
        }

        return nBody

//        for (a in args) {
//            val freeVariables = a.freeVariables(ctx)
//
//            for (p in fn.params) {
//                if (p.symbol in freeVariables) {
//                    // Perform alpha conversion to rename the bound variable
//                    fn = fn.convert(AlphaConversion(p.symbol)) as Lambda
//                    println("ALPHA: ${fn.name}")
//                }
//            }
//        }
//
//        if (aCount < pCount) {
//            val argBindings = fn.params.slice(IntRange(0, aCount)).zip(args)
//                .map { Pair(it.first.symbol, strategy.evaluate(ctx, it.second)) }
//
//            val nCtx = ctx.with(argBindings)
//
//            val partialArgs = fn.params.slice(IntRange(0, aCount)).zip(args)
//                .map { BetaConversion(it.first.symbol, it.second) }
//
//            val remaining = fn.params.subList(aCount, pCount)
//            val nBody = fn.body.convertAll(partialArgs)
//
//            return Lambda(remaining, nBody)
//                .convertAll(partialArgs)
//        } else if (pCount > aCount) {
//            return Type.Never
//        }
//
//        val argBindings = fn.params.zip(args)
//            .map { Pair(it.first.symbol, strategy.evaluate(ctx, it.second)) }
//
//        val nCtx = ctx.with(argBindings)
//
//        val betaReductions = fn.params.zip(args)
//            .map { BetaConversion(it.first.symbol, it.second) }
//
//        val nLambda = fn.convertAll(betaReductions) as Lambda
////        val nBody = nLambda.body.evaluate(ctx) as Term<*>
//        val nApp = Application(nLambda, argBindings.map { it.second })
//
//        return nApp.evaluate(nCtx)
    }

    override fun mirror(ctx: Ctx): IMirror<Obj> {
        TODO("Not yet implemented")
    }
}

@Suppress("UNCHECKED_CAST")
data class Ctx(val types: Map<TypeVar, IType> = emptyMap(), val terms: Map<TermVar, Term<*>> = emptyMap()) : Obj {
    constructor(vararg types: String) : this(types.map { TypeVar(it) to NominalType(it) }.toMap())

    override val name: String
        get() = "Γ(${types.pretty()} ; ${terms.pretty()})"

    operator fun <O: Obj> get(v: Var<O>) : O? {
        val result = types[Var(v.symbol)]

        if (result != null) return result as? O

        return terms[Var(v.symbol)] as? O
    }

    operator fun get(v: TypeVar) : IType? = types[v]
    operator fun get(v: TermVar) : Value? = null
    operator fun <O: Obj> get(v: String) : O? = get(Var<O>(v))

    fun containsTerm(symbol: String) : Boolean = terms.any { it.key.symbol == symbol }

    fun with(v: TypeVar, type: IType) : Ctx
        = Ctx(types + (v to type))

    fun with(v: String, type: IType) : Ctx
        = Ctx(types + (TypeVar(v) to type))

    fun with(v: TermVar, term: Term<*>) : Ctx
        = Ctx(types, terms + (v to term))

    fun with(v: String, term: Term<*>) : Ctx
        = Ctx(types, terms + (TermVar(v) to term))

    fun with(bindings: List<Pair<String, Term<*>>>) : Ctx
        = Ctx(types, terms + bindings.map { TermVar(it.first) to it.second })
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
                is Tuple -> TypeCheck(templateType, scrutineeType.head).evaluate(ctx)
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

    override fun mirror(ctx: Ctx): IMirror<Type> {
        TODO("Not yet implemented")
    }
}

interface ICase<O: Obj, R: Obj> : Term<R>, IMirror<O>

data class Case<O: Obj, R: Obj>(val pattern: IMirror<O>, val result: Term<R>) : ICase<O, R> {
    override val name: String = "(case ${pattern.name})"

    override fun evaluate(ctx: Ctx): R
        = result.evaluate(ctx)

    override fun match(ctx: Ctx, obj: O): Boolean
        = pattern.match(ctx, obj)

    override fun mirror(ctx: Ctx): IMirror<R> {
        TODO("Not yet implemented")
    }
}

data class Select<O: Obj, R: Value>(val cases: List<ICase<O, R>>, val scrutinee: O) : Term<R> {
    override val name: String = "(select ${scrutinee.name} ${cases.pretty()})"

    override fun evaluate(ctx: Ctx): R
        = cases.first { it.match(ctx, scrutinee) }.evaluate(ctx)


    override fun mirror(ctx: Ctx): IMirror<R> {
        TODO("Not yet implemented")
    }
}

interface Judgement : Term<BoolValue> {}

fun normalise(ctx: Ctx, obj: Obj) : Obj = when (obj) {
    is Normal<*> -> obj
    is Term<*> -> normalise(ctx, obj.evaluate(ctx))
    else -> obj
}

//data class TypeExists(val ctxTerm: Term<Ctx>, val binding: Var<IType>, val typeTuple: Tuple) : Judgement {
//    override val name: String = "(∃ ${ctxTerm.name}[${binding.name}:${typeTuple.name}])"
//
//    override fun evaluate(ctx: Ctx): BoolValue {
//        val ctxValue = ctxTerm.evaluate(ctx)
//        val
//    }
//
//    override fun mirror(ctx: Ctx): IMirror<BoolValue> {
//        TODO("Not yet implemented")
//    }
//}

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

        val nat = TypeVar("nat")
        val natType = NominalType("nat")
        val zero = Tuple(natType)
        val succ = Tuple(natType, listOf(natType))

        println(natType.name)
        println(zero.name)
        println(succ.name)

        val one = TypeInstance(succ, listOf(zero))

        println(one.name)

        val two = TypeInstance(succ, listOf(one))

        typeCheckT(ctx, natType, zero)
        typeCheckF(ctx, zero, natType)
        typeCheckT(ctx, natType, succ)
        typeCheckF(ctx, succ, natType)

        val x = Var<Value>("x")
        val xMirror = x.mirror(ctx)

        println(xMirror.name)

        val i = IntValue(2)
        val j = IntValue(3)
        val rangeMirror = IntRangeMirror(IntValue(3), IntValue(99), true)

        println(rangeMirror.name)

        println(rangeMirror.match(ctx, i))

        val case1 = Case(rangeMirror, BoolValue.True)
        val case2 = Case(ValueMirror(IntValue(2)), BoolValue.False)

        val select = Select(listOf(case1, case2), i)

        println(select.evaluate(ctx).name)

        val x2 = Var<Box<Obj>>("x2")
        val zBox = Box(z)
        val x2Unbox = Unbox(x2)
        val let = Let(x2, zBox, x2Unbox)

        println(ctx.name)

        println(let.name)
        println(let.evaluate(ctx).name)

        val y = TermVar("y")

        val id = Lambda(listOf(x), x)
        val id2 = Lambda(listOf(x, y), Product(x, y))

        println(id.name)

        val id0 = Application(id, listOf(natType), CallByName)

        println(id0.name)
        println(id0.evaluate(ctx).name)

        val b = TermVar("b")
        val c = Var<Lambda>("c")
        val f = Var<Lambda>("f")

        val cons = Lambda(listOf(a, b, f), Application(f, listOf(a, b)))
        val head = Lambda(listOf(c), Application(c, listOf(Lambda(listOf(a, b), a))))
        val tail = Lambda(listOf(c), Application(c, listOf(Lambda(listOf(a, b), b))))

        println(cons.name)
        println(head.name)
        println(tail.name)

        val headValue = Application(head, listOf(
            Application(cons, listOf(i, j))))

        val tailValue = Application(tail, listOf(
            Application(cons, listOf(i, j))))

        println(headValue.name)
        println(headValue.evaluate(ctx).name)

        println(tailValue.name)
        println(tailValue.evaluate(ctx).name)

        val β = TermVar("β")
        val beta = BetaConversion("β", IntValue(0))

        println(β.convert(beta).name)

        val twice = Var<Lambda>("twice")
        val twiceFn = Lambda(listOf(f, x), Application(f, listOf(Application(f, listOf(x)))))
        val ctx2 = Ctx(terms = mapOf(twice as TermVar to twiceFn))

        println(twiceFn.name)

        val twiceApp = Application(twiceFn, listOf(twiceFn))

        println(twiceApp.name)
        println(twiceApp.evaluate(ctx2).name)

        val twiceLambda = twiceApp.evaluate(ctx2) as Lambda

        val tlApp = Application(twiceFn, listOf(IntValue(0)))

        println(tlApp.name)
        println(tlApp.evaluate(ctx2).name)
    }

    @Test
    fun testTwice() {
        val x = TermVar("x")
        val f = Var<Lambda>("f")
//        val ctx = Ctx()
//
//        val inner = Lambda(listOf(x), x)
//        val outer = Lambda(listOf(x), inner)
//
//        println(inner.name)
//        println(outer.name)
//
//        val result = Application(outer, listOf(IntValue(0)))
//
//        println(result.name)
//        println(result.evaluate(ctx).name)

        val twice = Var<Lambda>("twice")
        val twiceFn = Lambda(listOf(f, x), Application(f, listOf(Application(f, listOf(x)))))
        val ctx = Ctx(terms = mapOf(twice as TermVar to twiceFn))

        val tApp = Application(twiceFn, listOf(twiceFn))

        println(tApp.name)
        println(tApp.evaluate(ctx).name)
    }

    @Test
    fun testCons() {
        val a = TermVar("a")
        val b = TermVar("b")
        val x = TermVar("x")
        val y = TermVar("y")
        val c = Var<Lambda>("c")
        val f = Var<Lambda>("f")
        val i = IntValue(0)
        val j = IntValue(1)

        val ctx = Ctx(terms = mapOf(x to i, y to j))

        val cons = Lambda(listOf(a, b, f), Application(f, listOf(a, b)))
        val head = Lambda(listOf(c), Application(c, listOf(Lambda(listOf(a, b), a))))
        val tail = Lambda(listOf(c), Application(c, listOf(Lambda(listOf(a, b), b))))

        val headValue = Application(head, listOf(
            Application(cons, listOf(x, y))))

        val tailValue = Application(tail, listOf(
            Application(cons, listOf(i, j))))

        println(headValue.name)
        println(headValue.evaluate(ctx).name)

        println(tailValue.name)
        val result = tailValue.evaluate(ctx)

        println(result.name)
    }

    @Test
    fun testId() {
        val ctx = Ctx()
        val x = TermVar("x")
        val id = Lambda(listOf(x), x)
        val id0 = Application(id, listOf(IntValue(0)))
        val idid = Application(id, listOf(id))

        println(idid.name)
        val result = idid.evaluate(ctx)

        println(result.name)

        val idid0 = Application(result as Lambda, listOf(IntValue(0)))

        println(idid0.evaluate(ctx).name)
    }

    @Test
    fun testPartial() {
        val ctx = Ctx()
        val x = TermVar("x")
        val y = TermVar("y")
        val first = Lambda(listOf(x, y), x)
        val last = Lambda(listOf(x, y), y)

        val fResult = Application(first, listOf(Missing, IntValue(0)))
        val lResult = Application(last, listOf(Missing, IntValue(1)))

        println(fResult.name)
        println(normalise(ctx, fResult).name)
        println(lResult.name)
        println(normalise(ctx, lResult).name)
    }
}
