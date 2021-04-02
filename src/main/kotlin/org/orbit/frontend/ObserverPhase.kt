package org.orbit.frontend

import org.orbit.core.ReifiedPhase
import org.orbit.core.SourceProvider
import org.orbit.core.nodes.ObserverNode
import org.orbit.util.Invocation

class ObserverPhase(override val invocation: Invocation) : ReifiedPhase<SourceProvider, SourceProvider> {
    override val inputType: Class<SourceProvider> = SourceProvider::class.java
    override val outputType: Class<SourceProvider> = SourceProvider::class.java

    override fun execute(input: SourceProvider): SourceProvider {
        val parserResult = invocation.getResult<Parser.Result>("Parser")
        val observerNodes = parserResult.ast.search(ObserverNode::class.java)
        var source = input.getSource()

        for (observerNode in observerNodes) {
            //val text = source.substring(observerNode.range)

            source = source.removeRange(observerNode.range)
        }

        val result = StringSourceProvider(source)

        //invocation.storeResult(result)

        return result
    }
}