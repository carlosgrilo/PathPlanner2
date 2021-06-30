package arwstate;

import exceptions.WrongOperationException;
import newWarehouse.Warehouse;
import orderpicking.GNode;
import pathfinder.Graph;
import whgraph.ARWGraph;
import whgraph.ARWGraphNode;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static whgraph.GraphNodeType.PRODUCT;

public class WarehouseState {
    private Warehouse warehouse;
    private ARWGraph arwGraph;
    private List<Agent> availableAgents;
    private Map<String, Agent> occupiedAgents;
    private List<Task> pendingTasks;

    private int minNumAgents;
    private int minAveragePicksPerTask;
    private int maxAveragePicksPerTask;

    private String defaultDestiny = "13.S.0.0";

    public WarehouseState(
            Warehouse warehouse,
            ARWGraph arwGraph,
            int minNumAgents,
            int minAveragePicksPerTask,
            int maxAveragePicksPerTask) {
        this.warehouse = warehouse;
        this.arwGraph = arwGraph;
        this.availableAgents = new LinkedList<>();
        this.occupiedAgents = new HashMap<>();
        this.pendingTasks = new LinkedList<>();
        this.minNumAgents = minNumAgents;
        this.minAveragePicksPerTask = minAveragePicksPerTask;
        this.maxAveragePicksPerTask = maxAveragePicksPerTask;
    }

    public void setWareHouseFromXMLFile(String filename) {
        try {
            String contents = new String(Files.readAllBytes(Paths.get(filename)));
            warehouse.createFromXML(contents);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Adiciona um novo agente disponível
//    public void addAvailableAgent(Agent agent) {
//        availableAgents.add(agent);
//    }

    public void addAvailableAgent(Agent agent) throws WrongOperationException {
        for (Agent availableAgent : availableAgents) {
            if(agent.getId().equals(availableAgent.getId())){
                throw new WrongOperationException("ERROR: Agent already available!");
            }
        }
        availableAgents.add(agent);
    }


    //Retira um agente caso os óculos sejam desligados
    public void removeAvailableAgent(String agentId) {
        for (Agent agent : availableAgents)
            if (agent.getId().equals(agentId)) {
                availableAgents.remove(agent);
                break;
            }
    }

    public Integer getNumberOfAvailableAgents() {
        return availableAgents.size();
    }

    public void releaseAgent(String agentId, float newX, float newY) {
        Agent agent = occupiedAgents.get(agentId);
        agent.setInitialX(newX);
        agent.setInitialY(newY);
        agent.setStartNode(null);
        agent.setEndNode(null);
        availableAgents.add(agent);
        occupiedAgents.remove(agentId);
    }

    public Integer getNumberOfPendingTasks() {
        return pendingTasks.size();
    }

    public boolean checkOccupiedAgent(String agentId) {
        return occupiedAgents.containsKey(agentId);
    }

    /**
     * Builds graph for a list of picks
     * @param picks
     * @return
     */
    public Graph<GNode> buildGraph(List<Pick> picks){

        //Constrói um grafo com todos os nós, incluindo os dos picks

        ARWGraph problemGraph = arwGraph.clone();
        //picksAtNode: estrutura que guarda os picks em cada nó
        Map<Integer, ArrayList<Pick>> picksAtNode = new HashMap<>();

        //Colocar os nós dos picks no grafo
        for (Pick pick : picks) {
            Point2D.Float rack = warehouse.getWms(pick.getOrigin());
            int numberOfNodes = problemGraph.getNumberOfNodes();
            int graphNodeID = problemGraph.insertNode(
                    new ARWGraphNode(numberOfNodes, rack.x, rack.y, PRODUCT)).getGraphNodeId();
            if (!picksAtNode.containsKey(graphNodeID)){
                picksAtNode.put(graphNodeID, new ArrayList<>());
            }
            picksAtNode.get(graphNodeID).add(pick);
            pick.setNode(problemGraph.findNode(graphNodeID));
        }

        //Transformação do grafo inicial para o grafo específico para o A*
        Graph<GNode> graph = problemGraph.getPathGraph();
        graph.setPicksAtNode(picksAtNode);

        return graph;
    }


    //////////////////////////////////////////////////

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public ARWGraph getArwGraph() {
        return arwGraph;
    }

    //Função para atribuição do grafo, lido de ficheiro ou criado/editado com editor
    public void setGraph(ARWGraph arwGraph) {
        this.arwGraph = arwGraph;
    }

    public List<Agent> getAvailableAgents() {
        return availableAgents;
    }

    public void setAvailableAgents(List<Agent> availableAgents) {
        this.availableAgents = availableAgents;
    }

    public Map<String, Agent> getOccupiedAgents() {
        return occupiedAgents;
    }

    public void setOccupiedAgents(Map<String, Agent> occupiedAgents) {
        this.occupiedAgents = occupiedAgents;
    }

    public List<Task> getPendingTasks() {
        return pendingTasks;
    }

    public void setPendingTasks(List<Task> pendingTasks) {
        this.pendingTasks = pendingTasks;
    }

    public int getMinNumAgents() {
        return minNumAgents;
    }

    public void setMinNumAgents(int minNumAgents) {
        this.minNumAgents = minNumAgents;
    }

    public int getMinAveragePicksPerTask() {
        return minAveragePicksPerTask;
    }

    public void setMinAveragePicksPerTask(int minAveragePicksPerTask) {
        this.minAveragePicksPerTask = minAveragePicksPerTask;
    }

    public int getMaxAveragePicksPerTask() {
        return maxAveragePicksPerTask;
    }

    public void setMaxAveragePicksPerTask(int maxAveragePicksPerTask) {
        this.maxAveragePicksPerTask = maxAveragePicksPerTask;
    }

    public void setDefaultDestiny(String defaultDestiny) {
        this.defaultDestiny = defaultDestiny;
    }

}
