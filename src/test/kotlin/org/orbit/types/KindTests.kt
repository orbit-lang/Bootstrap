package org.orbit.types

import org.junit.jupiter.api.Test
import org.orbit.core.components.Token
import org.orbit.core.components.TokenType
import org.orbit.core.components.TokenTypeProvider
import org.orbit.frontend.StringSourceProvider
import org.orbit.frontend.phase.Lexer
import org.orbit.util.Invocation
import org.orbit.util.Unix

//sealed class K(open val name: String) {
//    object InhabitedType : K("*")
//    open class HigherKind(val lhs: K, val rhs: K) : K("(${lhs.name}) -> (${rhs.name})")
//}
//
//data class VariadicTypeConstructorKind(val arity: Int) : K("(${"*".repeat(arity)}) -> *")
//
//val TC1 = VariadicTypeConstructorKind(1)
//val TC2 = VariadicTypeConstructorKind(2)
//
//data class TypeConstructorConstructor(val A: Ty) : K.HigherKind(K.InhabitedType, VariadicTypeConstructorKind(1))
//
//interface KindedEntity {
//    val kind: K
//}
//
interface Eq<T> {
    fun eq(a: T, b: T) : Boolean
}
//
fun <T> List<T>.contains(eq: Eq<T>, obj: T) : Boolean {
    return any { eq.eq(it, obj) }
}
//
//fun <T> List<T>.indexOf(eq: Eq<T>, obj: T) : Int {
//    return indexOfFirst { eq.eq(it, obj) }
//}
//
//object NominalEq : Eq<Ty> {
//    override fun eq(a: Ty, b: Ty): Boolean
//        = a.name == b.name
//}
//
//open class Ty(open val name: String, typeParameters: List<Ty> = emptyList(), private val props: List<Property> = emptyList()) : KindedEntity {
//    override val kind: K = K.InhabitedType
//    val typeMembers = typeParameters
//        .map { Ty("${name}::${it.name}", it.props) }
//
//    val properties = props
//        .map {
//            val pType = when (typeParameters.contains(NominalEq, it.type)) {
//                true -> typeMembers[typeParameters.indexOf(NominalEq, it.type)]
//                else -> it.type
//            }
//
//            Property(it.key, pType)
//        }
//
//    fun new(name: String) : V = V(this, name)
//    open fun dump() : String
//        = "${name}<${typeMembers.joinToString(", ") { it.name }}>(${properties.joinToString(", ") { it.dump() }})"
//}
//
//open class V(open val type: Ty, open val name: String)
//class Property(val key: String, val type: Ty) : Ty("${key}: ${type.name}") {
//    override fun dump(): String = name
//}
//class Argument(name: String, val value: V) : V(value.type, name)
//
//class Struct(properties: List<Property>) : Ty("{${properties.joinToString(",") { it.name }}}", properties)
//data class Instance(override val type: Struct, val values: List<V>) : V(type, "${type.name}(${values.joinToString(",") { it.name }})")
//
//typealias Kind = Function<Ty>
//typealias IType = () -> Ty
//typealias TConstructor1 = (Ty) -> Ty
//typealias TConstructor2 = (Ty, Ty) -> Ty
//
//typealias DependentType = (V) -> Ty
//typealias DependentTypeConstructor1 = (Ty, V) -> Ty
//
//typealias DependentKind = (String, Ty) -> Kind
//
//typealias KindConstructor = Function<KindedEntity>
//
//fun type(name: String) : Ty = Ty(name)
//fun typeConstructor1(name: String, type: Ty) : Ty = Ty("$name<${type.name}>")
//fun typeConstructor2(name: String, typeA: Ty, typeB: Ty) : Ty = Ty("$name<${typeA.name}, ${typeB.name}>")
//
//// StaticInt<width: Int>
////fun dependentKind(name: String, property: Property) : Kind
//
//fun dependentType(value: V) : Ty = Ty("${value.name} : ${value.type.name}")
//fun dependentTypeConstructor1(value: V, typeConstructor: TConstructor1) : Ty = Ty("${typeConstructor(value.type).name}[${dependentType(value).name}]")
//fun dependentTypeConstructor2(valueA: V, valueB: V, typeConstructor: TConstructor2) : Ty = Ty("${typeConstructor(valueA.type, valueB.type).name}[${valueA.name}, ${valueB.name}]")
//
//fun namedDependentType(arg: Argument) : Ty = Ty("${arg.name} :: ${dependentType(arg.value).name}")
//
//val listConstructor: TConstructor1 = partialReverse(::typeConstructor1, "List")
//val mapConstructor: TConstructor2 = partialReverseLast2(::typeConstructor2, "Map")
//
//inline fun <reified E: Eq<Ty>> typeCheck(eq: E, a: Ty, b: Ty) : Boolean
//    = eq.eq(a, b)

object STL {
    sealed interface Term {
        fun pretty(depth: Int = 0) : String

        fun Term.indent(depth: Int) : String = "\t".repeat(depth)

        data class Var(val name: String) : Term {
            override fun toString(): String = name

            override fun pretty(depth: Int): String = indent(depth) + "$name"
        }

        data class Ann(val v: Var, val type: Type) : Term {
            constructor(v: String, type: Type) : this(Var(v), type)

            override fun pretty(depth: Int): String {
                return indent(depth) + "(: ${v.pretty(depth)} ${type.name})"
            }
        }

        data class App(val lhs: Term, val rhs: Term) : Term {
            override fun pretty(depth: Int): String {
                return indent(depth) + "(${lhs.pretty(depth)} ${rhs.pretty(depth)})"
            }
        }

        data class Abs(val v: Ann, val term: Term) : Term {
            override fun toString(): String = "λ$v.$term"

            override fun pretty(depth: Int): String {
                return indent(depth) + "(λ ${v.pretty(depth)} ${term.pretty(depth)})"
            }
        }

        sealed interface BoolConst : Term {
            object True : BoolConst {
                override fun pretty(depth: Int): String = indent(depth) + "true"
            }

            object False : BoolConst {
                override fun pretty(depth: Int): String = indent(depth) + "false"
            }
        }

        data class IfElse(val cond: Term, val then: Term, val els: Term) : Term {
            override fun pretty(depth: Int): String {
                return indent(depth) + "(if " + cond.pretty(depth) +
                    "\n" + indent(depth + 1) + then.pretty(depth) +
                    "\n" + indent(depth + 1) + els.pretty(depth) + ")"
            }
        }
    }

    sealed interface Type {
        interface ClosedType<T: ClosedType<T>> : Type {
            val all: List<T>
        }

        object Anything : Type, ClosedType<Anything> {
            override val all: List<Anything> = listOf(Anything)

            override val name: String = "*"
        }

        object Never : Type, ClosedType<Never> {
            override val all: List<Never> = listOf(Never)

            init {
                throw Exception("Type could not be inferred")
            }

            override val name = "Never"
        }

        object NomEq : Eq<Type> {
            override fun eq(a: Type, b: Type): Boolean = when (a) {
                is SumType -> a == b
                else -> a.name == b.name
            }
        }

        abstract val name: String

        sealed interface Bool : Type, ClosedType<Bool> {
            override val all: List<Bool>
                get() = listOf(True, False)

            object AnyBool : Bool {
                override val name: String = "Bool"
            }

            object True : Bool {
                override val name: String = "Bool::True"
            }

            object False : Bool {
                override val name: String = "Bool::False"
            }
        }

        data class Lambda(val input: Type, val output: Type) : Type {
            override val name = "(${input.name} -> ${output.name})"

            override fun equals(other: Any?): Boolean = when (other) {
                is Type -> NomEq.eq(this, other)
                else -> false
            }
        }

        data class SumType(val constructors: List<Type>) : Type {
            override val name: String = "(${constructors.map(Type::name).joinToString(" | ")})"

            override fun equals(other: Any?): Boolean = when (other) {
                is SumType -> super.equals(other)
                is Type -> constructors.contains(NomEq, other)
                else -> false
            }
        }
    }

    class Universe(initialBindings: Map<Term.Var, Type> = emptyMap()) {
        private val bindings = mutableMapOf<Term.Var, Type>()

        constructor(globalContext: Universe) : this(globalContext.bindings)

        init {
            this.bindings.putAll(initialBindings)
        }

        fun bind(v: Term.Var, t: Type) {
            bindings[v] = t
        }

        fun get(v: Term.Var) : Type
            = bindings[v]!!
    }

    sealed interface Inference {
        object InferenceUtil {
            fun buildInference(term: Term) : Inference = when (term) {
                is Term.Var -> VarInference(term)
                is Term.Abs -> AbsInference(term)
                is Term.App -> AppInference(term)
                is Term.BoolConst -> BoolInference(term)
                is Term.IfElse -> IfElseInference(term)
                is Term.Ann -> AnnInference(term)
            }
        }

        class AnnInference(private val ann: Term.Ann) : Inference {
            override fun infer(universe: Universe): Type = ann.type
        }

        class IfElseInference(private val ifElse: Term.IfElse) : Inference {
            override fun infer(universe: Universe): Type {
                val cType = InferenceUtil.buildInference(ifElse.cond).infer(universe)

                if (cType !is Type.Bool) throw Exception("Condition in if/else expr must be of type Bool, found ${cType.name}")

                val iType = InferenceUtil.buildInference(ifElse.then).infer(universe)
                val eType = InferenceUtil.buildInference(ifElse.els).infer(universe)

                if (iType == eType) return iType

                return Type.SumType(listOf(iType, eType))
            }
        }

        class BoolInference(private val bool: Term.BoolConst) : Inference {
            override fun infer(universe: Universe): Type = when (bool) {
                is Term.BoolConst.True -> Type.Bool.True
                is Term.BoolConst.False -> Type.Bool.False
            }
        }

        class VarInference(private val v: Term.Var) : Inference {
            override fun infer(universe: Universe): Type = universe.get(v)
        }

        class AbsInference(private val abs: Term.Abs) : Inference {
            override fun infer(universe: Universe): Type {
                val localUniverse = Universe(universe)

                localUniverse.bind(abs.v.v, abs.v.type)

                val tType = InferenceUtil.buildInference(abs.term).infer(localUniverse)

                if (tType != abs.v.type)
                    throw Exception("Mismatched types in lambda ${abs.pretty()}. Expected ${abs.v.type.name}, found ${tType.name}")

                return Type.Lambda(abs.v.type, tType)
            }
        }

        class AppInference(private val app: Term.App) : Inference {
            override fun infer(universe: Universe): Type {
                val lType = InferenceUtil.buildInference(app.lhs).infer(universe)

                if (lType !is Type.Lambda)
                    throw Exception("Cannot apply term ${app.lhs} : ${lType.name}")

                val rType = InferenceUtil.buildInference(app.rhs).infer(universe)

                if (rType != lType.output) throw Exception("Mismatched types in function call. Expected ${lType.output.name}, found ${rType.name}")

                return lType.output
            }
        }

        fun infer(universe: Universe) : Type
    }
}

class λ {
    object TokenTypes : TokenTypeProvider {
        object Lambda : TokenType("λ", "λ", true, false, TokenType.Family.Keyword)
        object Variable : TokenType("Var", "[a-zA-Z]", true, false, TokenType.Family.Id)

        override fun getTokenTypes(): List<TokenType> {
            return listOf(Lambda, Variable, org.orbit.core.components.TokenTypes.Dot, org.orbit.core.components.TokenTypes.LParen, org.orbit.core.components.TokenTypes.RParen, org.orbit.core.components.TokenTypes.Colon)
        }
    }

    class Parser(private val tokens: List<Token>) {
        private var idx = 0

        private fun parseLambda() : STL.Term.Abs {
            idx += 1
            val v = parseVar() as? STL.Term.Var
                ?: throw Exception("Multiple variables in a single lambda are unsupported")
            idx += 1
            val e = parse()

            return STL.Term.Abs(STL.Term.Ann(v, STL.Type.Anything), e)
        }

        private fun parseVar() : STL.Term {
            val v = tokens[idx]
            idx += 1

            if (idx < tokens.count()) {
                val next = tokens[idx]
                if (next.type is λ.TokenTypes.Variable) {
                    // e.g. xy
                    idx += 1
                    return STL.Term.App(STL.Term.Var(v.text), STL.Term.Var(next.text))
                }
            }

            return STL.Term.Var(v.text)
        }

        private fun parseGrouped() : STL.Term {
            idx += 1
            val e = parse()
            idx += 1

            return e
        }

        fun parse() : STL.Term = when (val next = tokens[idx].type) {
            is λ.TokenTypes.Lambda -> parseLambda()
            is λ.TokenTypes.Variable -> parseVar()
            is org.orbit.core.components.TokenTypes.LParen -> parseGrouped()
            else -> throw Exception("Unrecognised token: $next")
        }
    }
}

class KindTests {
    @Test
    fun testIdentity() {
        val invocation = Invocation(Unix)
        val lexer = Lexer(invocation, λ.TokenTypes, false)
        val lexResult = lexer.execute(StringSourceProvider("λx.λy.y"))
        val parser = λ.Parser(lexResult.tokens)
        val prog = parser.parse()

        println(prog.pretty())

        val universe = STL.Universe()

        val inf = STL.Inference.InferenceUtil.buildInference(prog)
            .infer(universe)

        println(inf.name)
    }
}
