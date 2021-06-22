package requestmanagers;

import algorithms.SingleOrderDyn;
import algorithms.Solution;
import arwdatastruct.Agent;
import arwdatastruct.Order;
import arwdatastruct.Task;
import arwdatastruct.TaskOneAgentOneDestiny;
import gui.PathPlanner;
import newWarehouse.Warehouse;
import orderpicking.GNode;
import orderpicking.Pick;
import orderpicking.Request;
import pathfinder.Graph;
import whgraph.ARWGraph;
import whgraph.ARWGraphNode;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static whgraph.GraphNodeType.PRODUCT;

public abstract class RequestsManager {

    protected Warehouse warehouse;
    protected ARWGraph arwGraph;
    protected List<Agent> availableAgents;
    protected Map<String, Agent> occupiedAgents;
    protected List<Task> pendingTasks;

    protected String defaultDestiny = "13.S.0.0";
    protected int minNumAgents;
    protected int minAveragePicksPerTask;
    protected int maxAveragePicksPerTask;

    public RequestsManager() {
        this.warehouse = new Warehouse();
        this.arwGraph = new ARWGraph();
        this.availableAgents = new LinkedList<>();
        this.occupiedAgents = new HashMap<>();
        this.pendingTasks = new LinkedList<>();
        this.minNumAgents = PathPlanner.DEFAULT_MIN_NUM_AGENTS;
        this.minAveragePicksPerTask = PathPlanner.DEFAULT_MIN_AVERAGE_PICKS_PER_TASK;
        this.maxAveragePicksPerTask = PathPlanner.DEFAULT_MAX_AVERAGE_PICKS_PER_TASK;
    }

    /**
     * Loads the request received from the ERP in XML format
     * @param xmlERPRequest
     * @return
     */
    //Para já, o método devolve false se houver algum destino não definido no armazém, e true em caso contrário...
    public abstract boolean addRequest(String xmlERPRequest);

    /**
     * handles pending tasks
     */
    public abstract Map<Agent, String> handlePendingTasks();

    /////////////////////////////////////////////////////////////////////////////

    /**
     * If it is the last class from a request, returns the request
     * If not, returns null;
     * The agent is "released" at TOPIC_OPAVAIL in PathPlanner using method releaseAgent()
     * @param agentID
     * @param picks
     * @return
     */
    public Request closeTask(String agentID, List<Pick> picks) {

        Agent agent = occupiedAgents.get(agentID);
        Request request = agent.getTask().getRequest();
        request.decNumberTasksUnfinished();
        request.addSolvedPicks(picks);
        agent.setTask(null);

        return request.isSolved()? request : null;
    }

    public void setWareHouseFromXMLFile(String filename) {
        try {
            String contents = new String(Files.readAllBytes(Paths.get(filename)));
            warehouse.createFromXML(contents);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    //Função para atribuição do grafo, lido de ficheiro ou criado/editado com editor
    public void setGraph(ARWGraph arwGraph) {
        this.arwGraph = arwGraph;
    }

    public void setDefaultDestiny(String defaultDestiny) {
        this.defaultDestiny = defaultDestiny;
    }

    //Adiciona um novo agente quando ficar disponível
    public void addAvailableAgent(Agent agent) {
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

    public Integer getNumberOfPendingOrders() {
        return pendingTasks.size();
    }

    public boolean checkOccupiedAgent(String agentId) {
        return occupiedAgents.containsKey(agentId);
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

    /////////////////////////////////////////////////////////////////////

    protected Graph<GNode> buildGraph(List<Pick> picks){

        //Constrói um grafo com todos os nós, incluindo os picks de todas as tasks a processar a seguir

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

    //Atribuir tasks aos agentes (critério: centro de massa)
    protected void assignOneDestinyOneAgentTasks(List<TaskOneAgentOneDestiny> tasksToBeProcessed, List<Agent> agents){
        for (TaskOneAgentOneDestiny task : tasksToBeProcessed) {

            Graph<GNode> graph = buildGraph(task.getPicks());
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

    //Calcular a rota para cada task
    protected Map<Agent, String> solveOneDestinyOneAgentTasks(List<TaskOneAgentOneDestiny> tasksToBeProcessed){
        SingleOrderDyn orderDyn = new SingleOrderDyn();
        Map<Agent, String> tasksAssignment = new HashMap<>();

        for (TaskOneAgentOneDestiny task : tasksToBeProcessed) {
            Solution solution = orderDyn.solve(task);
            Agent agent = task.getAgent();
            task.setRoute(solution.getRoute(agent));
            tasksAssignment.put(agent, task.XMLPath());
            occupiedAgents.put(agent.getId(), agent);
            availableAgents.remove(agent);
        }

        return tasksAssignment;
    }

    protected List<Pick> buildRequestPicksList(Request request){
        List<Pick> picks = new LinkedList<>();
        for (Order order : request.getOrders()) {
            for (Pick pick : order.getPicks()) {
                picks.add(pick);
            }
        }
        return picks;
    }

}
