package orderpicking;

import pathfinder.Scorer;

public class DistanceScorer implements Scorer<GNode> {
    @Override
    public double computeCost(GNode from, GNode to){
        return Math.sqrt(Math.pow(from.getX() - to.getX(), 2) + Math.pow(from.getY() - to.getY(), 2));
    }
}