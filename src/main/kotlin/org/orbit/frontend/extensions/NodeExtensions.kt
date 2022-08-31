package org.orbit.frontend.extensions

import org.orbit.core.AnySerializable
import org.orbit.core.nodes.INode
import org.orbit.core.nodes.NodeAnnotationMap
import org.orbit.core.nodes.NodeAnnotationTag
import org.orbit.frontend.rules.ParseRule
import org.orbit.util.getKoinInstance

fun <N: INode> N.toParseResultSuccess() : ParseRule.Result.Success<N> {
    return ParseRule.Result.Success(this)
}

operator fun <N: INode> N.unaryPlus() : ParseRule.Result.Success<N> {
    return this.toParseResultSuccess()
}

//fun <T: AnySerializable> INode.annotate(value: T, tag: NodeAnnotationTag<T>, mergeOnConflict: Boolean = false) {
//    val nodeAnnotationMap = getKoinInstance<NodeAnnotationMap>()
//
//    nodeAnnotationMap.annotate(this, value, tag, mergeOnConflict)
//}