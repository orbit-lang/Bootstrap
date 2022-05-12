package org.orbit.types.next.components

interface IValue : TypeComponent {
    override val kind: Kind get() = IntrinsicKinds.Value
    val type: TypeComponent
}