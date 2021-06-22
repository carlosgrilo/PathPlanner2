package arwdatastruct;

import orderpicking.DistanceScorer;
import orderpicking.GNode;
import pathfinder.Graph;
import pathfinder.Route;
import pathfinder.RouteFinder;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.neighboursearch.PerformanceStats;

public class AStarDistance extends EuclideanDistance {

    Graph<GNode> graph;

    public AStarDistance(Graph<GNode> graph) {
        this.graph = graph;
    }

    @Override
    public double distance(Instance first, Instance second) {

        RouteFinder routeFinder = new RouteFinder<>(graph, new DistanceScorer(), new DistanceScorer());

        String firstID = new Integer((int) first.value(2)).toString();
        String secondID = new Integer((int) second.value(2)).toString();

        Route r = routeFinder.findRoute(graph.getNode(firstID), graph.getNode(secondID));

        return r.getCost();
    }

    @Override
    public double distance(Instance first, Instance second, PerformanceStats stats) {
        return this.distance(first, second);
    }
}
