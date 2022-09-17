package org.orbit.backend.typegen.utils

import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatformTools
import org.orbit.backend.typegen.components.walkers.IPrecessNodeWalker
import org.orbit.core.nodes.INode
import org.orbit.precess.frontend.components.nodes.IPrecessNode
import org.orbit.util.getKoinInstance
import kotlin.reflect.KClass

object TypeGenUtil {
    inline fun <reified N: INode, reified P: IPrecessNode> walk(node: N) : P {
        //println("NODE: $node")
        val walker = KoinPlatformTools.defaultContext().get().get<IPrecessNodeWalker<N, P>>(named("${N::class.java.simpleName}${P::class.java.simpleName}"))

        return walker.walk(node)
    }

    fun <N: INode, P: IPrecessNode> walk(node: N, clazz: KClass<IPrecessNodeWalker<N, P>>) : P {
        val walker = getKoinInstance(clazz)

        return walker.walk(node)
    }

    inline fun <reified N: INode, reified P: IPrecessNode> walkAll(nodes: List<N>) : List<P>
        = nodes.map { walk(it) }

    fun <N: INode, P: IPrecessNode> walkAll(nodes: List<N>, clazz: KClass<IPrecessNodeWalker<N, P>>) : List<P>
        = nodes.map { walk(it, clazz) }
}