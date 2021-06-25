package solvers;

import arwstate.Agent;
import pathfinder.Route;

import java.util.HashMap;
import java.util.Map;

public class Solution {

    private Map<Agent, Route> routes;

    public Solution(){
        this.routes = new HashMap<>();
    }

    public Map<Agent, Route> getRoutes() {
        return routes;
    }

    public void setRoutes(Map<Agent, Route> routes) {
        this.routes = routes;
    }

    public void addRoute(Agent agent, Route route) {
        this.routes.put(agent, route);
    }

    public Route getRoute(Agent agent){
        return routes.get(agent);
    }

}
