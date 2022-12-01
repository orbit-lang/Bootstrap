package org.orbit.backend.codegen.swift.utils

enum class SwiftEntity {
    Class, Struct, Enum, Protocol;

    override fun toString(): String
        = name.lowercase()
}

enum class SwiftMutability {
    Var, Let;

    override fun toString(): String
        = name.lowercase()
}

object EntityWriter {
    private fun write(kind: SwiftEntity, entityName: String, typeParameters: String, properties: List<String>) : String = """
        |public $kind $entityName <$typeParameters> {
        |   ${properties.joinToString("\n\t")}
        |   public init() {}
        |}
        |
        """.trimMargin()

    private fun write(kind: SwiftEntity, entityName: String, properties: List<String>) : String = """
        |public $kind $entityName {
        |   ${properties.joinToString("\n\t")}
        |   public init() {}
        |}
        |
        """.trimMargin()

    fun write(kind: SwiftEntity, entityName: String, typeParameters: List<String> = emptyList(), properties: List<String> = emptyList()) : String = when (typeParameters.isEmpty()) {
        true -> write(kind, entityName, properties)
        else -> write(kind, entityName, typeParameters.joinToString(", "), properties)
    }

    fun writeWithConformance(kind: SwiftEntity, entityName: String, protocolName: String) : String = """
        |public $kind $entityName : $protocolName {
        |   public init() {}
        |}
        |
    """.trimMargin()

    fun writeProtocol(protocolName: String) : String = """
        |protocol $protocolName {}
        |
    """.trimMargin()

    fun writeSingleton(entityName: String) : String = """
        |public class $entityName {
        |   public static let only = $entityName()
        |   public init() {}
        |}
        |
    """.trimMargin()
}

object PropertyWriter {
    fun write(mutability: SwiftMutability, propertyName: String, propertyType: String) : String
        = "$mutability $propertyName: $propertyType"
}