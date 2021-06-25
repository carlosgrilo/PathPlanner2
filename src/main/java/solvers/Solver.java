package solvers;

import arwstate.Agent;
import arwstate.WarehouseState;
import arwstate.TaskOneAgentOneDestiny;
import newWarehouse.Warehouse;
import orderpicking.GNode;
import pathfinder.Graph;
import whgraph.ARWGraph;
import whgraph.ARWGraphNode;

import java.util.List;
import java.util.Map;

public abstract class Solver {

    protected WarehouseState warehouseState;

    public Solver(WarehouseState warehouseState) {
        this.warehouseState = warehouseState;
    }

    /**
     * Solves pending tasks
     */
    public abstract Map<Agent, String> solve();


    /**
     * Assigns tasks to agents (criterium: mass center)
     * @param tasksToBeProcessed
     * @param agents
     */
    protected void assignTasksToAgents(List<TaskOneAgentOneDestiny> tasksToBeProcessed, List<Agent> agents){

        ARWGraph arwGraph = warehouseState.getArwGraph();
        Warehouse warehouse = warehouseState.getWarehouse();

        for (TaskOneAgentOneDestiny task : tasksToBeProcessed) {

            Graph<GNode> graph = warehouseState.buildGraph(task.getPicks());
            task.setGraph(graph);
            task.computeMassCenter();
            double smallerDistanceToMassCenter = Double.MAX_VALUE;
            Agent assignedAgent = null;
            for (Agent agent : agents) {
                double distance = Math.sqrt(
                        Math.pow(agent.getInitialX() - task.getMassCenterX(), 2) + Math.pow(agent.getInitialY() - task.getMassCenterY(), 2));
                if (distance < smallerDistanceToMassCenter) {
                    smallerDistanceToMassCenter = distance;
                    assignedAgent = agent;
                }
            }
            ARWGraphNode startNode = arwGraph.findClosestNode(
                    assignedAgent.getInitialX(),
                    assignedAgent.getInitialY());
            ARWGraphNode endNode = arwGraph.findClosestNode(
                    warehouse.getWms(task.getDestiny()).x,
                    warehouse.getWms(task.getDestiny()).y);
            assignedAgent.setStartNode(Integer.toString(startNode.getGraphNodeId()));
            assignedAgent.setEndNode(Integer.toString(endNode.getGraphNodeId()));

            agents.remove(assignedAgent);
            task.setAgent(assignedAgent);
            assignedAgent.setTask(task);
        }

    }

}
