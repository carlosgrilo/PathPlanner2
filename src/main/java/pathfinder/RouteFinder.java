package pathfinder;

import java.util.*;


public class RouteFinder<T extends GraphNode> {
    private final Graph<T> graph;
    private final Scorer<T> nextNodeScorer;
    private final Scorer<T> targetScorer;

    public RouteFinder(Graph<T> graph, Scorer<T> nextNodeScorer, Scorer<T> targetScorer) {
        this.graph = graph;
        this.nextNodeScorer = nextNodeScorer;
        this.targetScorer = targetScorer;
    }

    public Route findRoute(T from, T to) throws IllegalStateException {
        Map<T, RouteNode<T>> allNodes = new HashMap<>();
        Queue<RouteNode> openSet = new PriorityQueue<>();
        double finalScore= 0.0;
        RouteNode<T> start = new RouteNode<>(from, null, 0d, targetScorer.computeCost(from, to));
        allNodes.put(from, start);
        openSet.add(start);

        while (!openSet.isEmpty()) {
            //System.out.println("Open Set contains: " + openSet.stream().map(RouteNode::getCurrent).collect(Collectors.toSet()));
            RouteNode<T> next = openSet.poll();
            //System.out.println("Looking at node: " + next);
            if (next.getCurrent().equals(to)) {
                //System.out.println("Found our destination!");

                List<T> route = new ArrayList<>();
                RouteNode<T> current = next;
                do {
                    route.add(0, current.getCurrent());
                    current = allNodes.get(current.getPrevious());
                } while (current != null);

                //System.out.println("Route: " + route);
                Route rota = new Route(route,next.getRouteScore());
                return rota;
            }

            graph.getConnections(next.getCurrent()).forEach(connection -> {
                double newScore=(next.getRouteScore() + nextNodeScorer.computeCost(next.getCurrent(), connection));
                RouteNode<T> nextNode = allNodes.getOrDefault(connection, new RouteNode<>(connection));
                allNodes.put(connection, nextNode);

                if (nextNode.getRouteScore() > newScore) {
                    nextNode.setPrevious(next.getCurrent());
                    nextNode.setRouteScore(newScore);
                    nextNode.setEstimatedScore(newScore + targetScorer.computeCost(connection, to));
                    openSet.add(nextNode);
                    //System.out.println("Found a better route to node: " + nextNode);
                }

            });
        }

        throw new IllegalStateException("No route found");
    }

    public ArrayList<T> reverseRoute(Route r){
        ArrayList<T> reverse = new ArrayList(r.getNodes());
        Collections.reverse(reverse);
        return reverse;
    }

}