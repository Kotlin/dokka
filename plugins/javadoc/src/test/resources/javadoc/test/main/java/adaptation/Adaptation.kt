package adaptation

import app.MainApp
import model.InteriorNode
import model.ModelGraph
import org.javatuples.Pair
import transformation.Transformation

class Adaptation {
    private val log: org.apache.log4j.Logger = org.apache.log4j.Logger.getLogger(Adaptation::class.java.name)
    fun transform(graph: ModelGraph, transformation: Transformation): Pair<ModelGraph, Boolean> {
        return transform(graph, transformation, false)
    }

    private fun transform(
        graph: ModelGraph,
        transformation: Transformation,
        prevResult: Boolean
    ): Pair<ModelGraph, Boolean> {
        var model: ModelGraph = graph
        val interiors: Collection<InteriorNode> = graph.getInteriors()
        for (i in interiors) {
            try {
                if (transformation.isConditionCompleted(graph, i)) {
                    model = transformation.transformGraph(model, i)
                    log.info(
                        "Executing transformation: " + transformation.getClass().getSimpleName()
                            .toString() + " on interior" + i.getId()
                    )
                    Thread.sleep(1000)
                    return transform(model, transformation, true)
                }
            } catch (e: Exception) {
                log.error(
                    "Transformation " + transformation.getClass().getSimpleName()
                        .toString() + " returned an error: " + e.toString() + " for interior: " + i.toString()
                )
            }
        }
        return Pair(model, prevResult)
    }

    inner class AdaptationInternalClass internal constructor(var param: Int) {

        inner class AdaptationInternalInternalClass

    }

    class AdaptationInternalStaticClass internal constructor(var param: Int)
}