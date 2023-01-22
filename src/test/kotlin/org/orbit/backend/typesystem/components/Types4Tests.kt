package org.orbit.backend.typesystem.components

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.StringQualifier
import org.koin.dsl.module
import org.orbit.util.assertIs
import org.orbit.util.getKoinInstance
import kotlin.test.assertEquals
import kotlin.test.assertTrue

typealias AnyElement = TElement<*>

fun <K, A, B> Map<K, A>.mapMap(f: (A) -> B) : Map<K, B> {
    val m = mutableMapOf<K, B>()
    for (entry in entries) {
        m[entry.key] = f(entry.value)
    }

    return m
}

sealed interface TElement<Self: TElement<Self>> {
    val id: String

    fun sub(old: AnyElement, new: AnyElement) : Self
}

data class Universe private constructor(val idx: Int = 0, val elements: List<AnyElement>) : TElement<Universe> {
    companion object {
        val root = Universe(0, emptyList())
    }

    override val id: String = "U$idx"

    fun add(element: AnyElement) : Universe
        = Universe(idx, elements + element)

    operator fun get(element: AnyElement) : Boolean
        = elements.contains(element)

    fun fork() : Universe
        = Universe(idx + 1, elements)
            .add(this)

    override fun sub(old: AnyElement, new: AnyElement): Universe
        = Universe(idx, elements.map { it.sub(old, new) })

    override fun equals(other: Any?): Boolean = when (other) {
        is Universe -> other.idx == idx
        else -> false
    }

    override fun toString(): String = id
}

data class Ctx(val bindings: Map<Value, AnyElement> = emptyMap()) : TElement<Ctx> {
    override val id: String = "(Γ: ${bindings.map { "${it.key}:${it.value}" }})"

    fun bind(value: Value, type: AnyElement) : Ctx
        = Ctx(bindings + (value to type))

    operator fun get(binding: Value) : AnyElement?
        = bindings[binding]

    override fun sub(old: AnyElement, new: AnyElement): Ctx
        = Ctx(bindings.mapMap { it.sub(old, new) })

    override fun toString(): String = id
}

data class Type(override val id: String) : TElement<Type> {
    override fun equals(other: Any?): Boolean = when (other) {
        is Type -> other.id == id
        else -> false
    }

    override fun sub(old: AnyElement, new: AnyElement): Type = when (old) {
        this -> new as Type
        else -> this
    }

    override fun toString(): String = id
}

data class Value(override val id: String) : TElement<Value> {
    override fun equals(other: Any?): Boolean = when (other) {
        is Value -> other.id == id
        else -> false
    }

    override fun sub(old: AnyElement, new: AnyElement): Value
        = this

    override fun toString(): String = id
}

data class Function(val arg: String, val argType: AnyElement?, val body: AnyElement) : TElement<Function> {
    override val id: String = when (argType) {
        null -> "(λ$arg.${body.id})"
        else -> "(λ($arg:${argType.id}).${body.id})"
    }

    override fun sub(old: AnyElement, new: AnyElement): Function {
        val nArgType = argType?.sub(old, new)
        val nBody = body.sub(old, new)

        return Function(arg, nArgType, nBody)
    }

    fun withArgType(t: Type) : Function
        = Function(arg, t, body)

    fun generify(typeParameter: String) : Function
        = Function(typeParameter, null,
            this.withArgType(Type(typeParameter)))

    override fun equals(other: Any?): Boolean = when (other) {
        is Function -> other.id == id
        else -> false
    }

    override fun toString(): String = id
}

data class Signature(val domain: AnyElement, val codomain: AnyElement) : TElement<Signature> {
    override val id: String = "($domain -> $codomain)"

    override fun sub(old: AnyElement, new: AnyElement): Signature
        = Signature(domain.sub(old, new), codomain.sub(old, new))

    override fun toString(): String = id
}

data class Product(val left: AnyElement, val right: AnyElement) : TElement<Product> {
    override val id: String = "($left * $right)"

    override fun sub(old: AnyElement, new: AnyElement): Product
        = Product(left.sub(old, new), right.sub(old, new))

    override fun toString(): String = id
}

data class Pattern(val pair: Product, val lOp: Function, val rOp: Function) : TElement<Pattern> {
    override val id: String = """
        $pair\.0 :: $lOp
        $pair\.1 :: $rOp
    """.trimIndent()

    override fun sub(old: AnyElement, new: AnyElement): Pattern
        = Pattern(pair.sub(old, new), lOp.sub(old, new), rOp.sub(old, new))
}

sealed interface Meta : TElement<Meta>

object Anything : Meta {
    override val id: String = "*"

    override fun sub(old: AnyElement, new: AnyElement): Anything
        = this

    override fun toString(): String = id
}

data class Error(val message: String) : Meta {
    override val id: String = "!!! $message"

    override fun sub(old: AnyElement, new: AnyElement): Error
        = this

    override fun toString(): String = message
}

sealed interface TInference<K: AnyElement> {
    fun infer(ctx: Ctx, expr: K) : AnyElement
}

object ValueInference : TInference<Value> {
    override fun infer(ctx: Ctx, expr: Value): AnyElement
        = ctx[expr] ?: Error("Undefined value: $expr")
}

object FunctionInference : TInference<Function> {
    override fun infer(ctx: Ctx, expr: Function): AnyElement = when (val domain = expr.argType) {
        null -> Signature(Anything, infer(ctx, expr.body))
        else -> {
            val nCtx = ctx.bind(Value(expr.arg), domain)
            val codomain = infer(nCtx, expr.body)

            Signature(domain, codomain)
        }
    }
}

object ProductInference : TInference<Product> {
    override fun infer(ctx: Ctx, expr: Product): AnyElement {
        val lType = infer(ctx, expr.left)
        val rType = infer(ctx, expr.right)

        return Product(lType, rType)
    }
}

object AnyInference : TInference<AnyElement> {
    override fun infer(ctx: Ctx, expr: AnyElement): AnyElement = when (expr) {
        is Value -> ValueInference.infer(ctx, expr)
        is Function -> FunctionInference.infer(ctx, expr)
        is Product -> ProductInference.infer(ctx, expr)
        else -> TODO("INFER $expr")
    }
}

private fun infer(ctx: Ctx, expr: AnyElement) : AnyElement = when (val type = AnyInference.infer(ctx, expr)) {
    is Error -> throw Exception(type.message)
    else -> type
}

sealed interface TCheck<T: AnyElement> {
    fun check(ctx: Ctx, expr: T, expected: AnyElement)
}

object ValueCheck : TCheck<Value> {
    override fun check(ctx: Ctx, expr: Value, expected: AnyElement) {
        val type = infer(ctx, expr)
        if (type != expected) {
            throw Exception("Type error: Mismatched types $type & $expected")
        }
    }
}

object AnyCheck : TCheck<AnyElement> {
    override fun check(ctx: Ctx, expr: AnyElement, expected: AnyElement) = when (expr) {
        is Value -> ValueCheck.check(ctx, expr, expected)
        else -> TODO("CHECK $expr")
    }
}

internal class Types4Tests {
    private val resultQualifier = StringQualifier("result")

    @BeforeEach
    fun setUp() {
        startKoin {}
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun test1() {
        val t = Type("T")
        val id = Function("x", t, Value("x"))
        val idWrapper = Function("T", null, id)

        println(idWrapper.id)
    }

    private fun `Given a Universe`(universe: Universe) {
        loadKoinModules(module {
            single { universe }
        })
    }

    private fun `Given an empty Ctx`() {
        loadKoinModules(module {
            single { Ctx() }
        })
    }

    private fun `Given a Ctx with bindings`(bindings: Map<Value, Type>) {
        loadKoinModules(module {
            single { Ctx(bindings) }
        })
    }

    private fun `When the Universe is forked`() {
        val universe = getKoinInstance<Universe>()
        val result = universe.fork()

        loadKoinModules(module {
            single(resultQualifier) { result }
        })
    }

    private inline fun <reified T: AnyElement> `When type inference is performed`() {
        val ctx = getKoinInstance<Ctx>()
        val t = getKoinInstance<T>()
        val res = AnyInference.infer(ctx, t)

        loadKoinModules(module {
            single(resultQualifier) { res }
        })
    }

    private fun `Then the forked Universe should have index`(expected: Int) {
        val result = getKoinInstance<Universe>(resultQualifier)

        assertEquals(expected, result.idx)
    }

    private fun `And the forked Universe should contain the initial Universe`() {
        val result = getKoinInstance<Universe>(resultQualifier)

        assertTrue(result[Universe.root])
    }

    private fun `Then an Error should be raised`() {
        val res = getKoinInstance<AnyElement>(resultQualifier)

        assertIs<Error>(res)
    }

    private fun `Then Type should match`(expected: AnyElement) {
        val res = getKoinInstance<AnyElement>(resultQualifier)

        assertEquals(expected, res)
    }

    @Test
    fun `Should clone Universe with next index`() {
        `Given a Universe`(Universe.root)
        `When the Universe is forked`()
        `Then the forked Universe should have index`(1)
        `And the forked Universe should contain the initial Universe`()
    }

    private fun `Given a Value`(v: String) {
        loadKoinModules(module {
            single { Value(v) }
        })
    }

    @Test
    fun `Should infer Error for undefined Value`() {
        `Given an empty Ctx`()
        `Given a Value`("x")
        `When type inference is performed`<Value>()
        `Then an Error should be raised`()
    }

    @Test
    fun `Should infer Type for defined Value`() {
        val x = Value("x")
        val t = Type("T")

        `Given a Ctx with bindings`(mapOf(x to t))
        `Given a Value`("x")
        `When type inference is performed`<Value>()
        `Then Type should match`(t)
    }

    private fun `Given a function`(f: Function) {
        loadKoinModules(module {
            single { f }
        })
    }

    private fun `When generified with type parameter`(t: String) {
        val f = getKoinInstance<Function>()
        val g = f.generify(t)

        loadKoinModules(module {
            single(resultQualifier) { g }
        })
    }

    private fun `Then the resulting function matches`(h: Function) {
        val g = getKoinInstance<Function>(resultQualifier)

        assertEquals(h, g)
    }

    @Test
    fun `Should get a generic version of 'id' function`() {
        val id = Function("x", null, Value("x"))

        `Given a function`(id)
        `When generified with type parameter`("T")
        `Then the resulting function matches`(Function("T", null, id.withArgType(Type("T"))))
    }

    @Test
    fun `Should infer untyped Function to be Anything`() {
        val f = Function("x", Anything, Value("x"))
        val s = Signature(Anything, Anything)

        `Given an empty Ctx`()
        `Given a function`(f)
        `When type inference is performed`<Function>()
        `Then Type should match`(s)
    }

    @Test
    fun `Should infer body type in Function with typed arg`() {
        val x = Type("X")
        val f = Function("x", x, Value("x"))
        val s = Signature(x, x)

        `Given an empty Ctx`()
        `Given a function`(f)
        `When type inference is performed`<Function>()
        `Then Type should match`(s)
    }

    @Test
    fun `Should infer body type of generic Function`() {
        // TODO - `*` in `(*) -> (T -> T)` is wrong; should be something like `(T:U) -> (T -> T)` where `U` : Universe
        val x = Type("X")
        val t = Type("T")
        val f = Function("x", x, Value("x"))
        val g = f.generify("T")
        val s = Signature(Anything, Signature(t, t))

        `Given an empty Ctx`()
        `Given a function`(g)
        `When type inference is performed`<Function>()
        `Then Type should match`(s)
    }

    private fun `Given a product`(p: Product) {
        loadKoinModules(module {
            single { p }
        })
    }

    @Test
    fun `Should infer well-typed Product`() {
        val t = Type("T")
        val x = Value("x")

        `Given a Ctx with bindings`(mapOf(x to t))
        `Given a product`(Product(x, x))
        `When type inference is performed`<Product>()
        `Then Type should match`(Product(t, t))
    }

    private fun `When type checking is performed`(expected: AnyElement) {
        val ctx = getKoinInstance<Ctx>()
        val value = getKoinInstance<Value>()

        try {
            AnyCheck.check(ctx, value, expected)
            loadKoinModules(module {
                single(resultQualifier) { expected }
            })
        } catch (ex: Exception) {
            loadKoinModules(module {
                single(resultQualifier) { ex }
            })
        }
    }

    private fun `Then an error should be thrown`(expected: String) {
        val res = getKoinInstance<Exception>(resultQualifier)

        assertEquals(expected, res.message)
    }

    private fun `Then type check result should match`(expected: AnyElement) {
        val res = getKoinInstance<AnyElement>(resultQualifier)

        assertEquals(expected, res)
    }

    @Test
    fun `Should fail type check for undefined Value`() {
        `Given an empty Ctx`()
        `Given a Value`("x")
        `When type checking is performed`(Anything)
        `Then an error should be thrown`("Undefined value: x")
    }

    @Test
    fun `Should fail type check for mismatched Value`() {
        val t = Type("T")
        val u = Type("U")
        val x = Value("x")

        `Given a Ctx with bindings`(mapOf(x to t))
        `Given a Value`("x")
        `When type checking is performed`(u)
        `Then an error should be thrown`("Type error: Mismatched types T & U")
    }

    @Test
    fun `Should type check well-typed Value`() {
        val t = Type("T")
        val x = Value("x")

        `Given a Ctx with bindings`(mapOf(x to t))
        `Given a Value`("x")
        `When type checking is performed`(t)
        `Then type check result should match`(t)
    }
}