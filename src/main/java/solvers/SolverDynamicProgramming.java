package solvers;

import arwstate.Agent;
import arwstate.WarehouseState;
import arwstate.Task;
import arwstate.TaskOneAgentOneDestiny;
import newWarehouse.Warehouse;
import orderpicking.DistanceScorer;
import pathfinder.GraphNode;
import pathfinder.Route;
import pathfinder.RouteFinder;
import tsp.TspDynamicProgrammingIterative;
import whgraph.ARWGraph;

import java.util.*;

/**
 * - Tasks are assigned to agents based on distance to cluster mass center;
 * - Each task is solved using Dynamic Programming;
 */
public class SolverDynamicProgramming extends Solver{

    public SolverDynamicProgramming(WarehouseState warehouseState) {
        super(warehouseState);
    }

    /**
     * Solves pending tasks
     */
    @Override
    public Map<Agent, String> solve(){

        Warehouse warehouse = warehouseState.getWarehouse();
        ARWGraph arwGraph = warehouseState.getArwGraph();
        List<Agent> availableAgents = warehouseState.getAvailableAgents();
        List<Task> pendingTasks = warehouseState.getPendingTasks();

        if (warehouse == null || arwGraph == null || arwGraph.getNumberOfNodes() == 0) {
            System.out.println("Armazem ou grafo não definido!");
            return null; //TODO Lançar exceção apropriada
        }

        if (availableAgents.size() == 0 || pendingTasks.size() == 0) {
            return null; //TODO Lançar exceção apropriada
        }

        int numberOfTasksToProcess = Math.min(availableAgents.size(), pendingTasks.size());
        List<Agent> auxAgents = availableAgents.subList(0, numberOfTasksToProcess);
        List<Agent> agents = new LinkedList<>(auxAgents);

        List<TaskOneAgentOneDestiny> tasksToBeProcessed = new LinkedList<>();
        for (int i = 0; i < numberOfTasksToProcess; i++) {
            tasksToBeProcessed.add((TaskOneAgentOneDestiny) pendingTasks.remove(0));
        }

        assignTasksToAgents(tasksToBeProcessed, agents);

        Map<Agent, String> tasksAssignment = solveTasks(tasksToBeProcessed);

        return tasksAssignment;

    }

    /**
     * Computes the route for each task to be processed
     * @param tasksToBeProcessed
     * @return
     */
    protected Map<Agent, String> solveTasks(List<TaskOneAgentOneDestiny> tasksToBeProcessed){

        List<Agent> availableAgents = warehouseState.getAvailableAgents();
        Map<String, Agent> occupiedAgents = warehouseState.getOccupiedAgents();

        Map<Agent, String> tasksAssignment = new HashMap<>();

        for (TaskOneAgentOneDestiny task : tasksToBeProcessed) {
            Solution solution = solveTask(task);
            Agent agent = task.getAgent();
            task.setRoute(solution.getRoute(agent));
            tasksAssignment.put(agent, task.XMLPath());
            occupiedAgents.put(agent.getId(), agent);
            availableAgents.remove(agent);
        }

        return tasksAssignment;
    }

    public Solution solveTask(TaskOneAgentOneDestiny task){

        RouteFinder routeFinder = new RouteFinder<>(task.getGraph(), new DistanceScorer(), new DistanceScorer());
        //Constroi matriz de distâncias com m x m, m = nnosproduto + entrada+saída
        //a 1ª linha serve para a distância entre o nó de partida e cada um dos nós de produto
        //a última linha tem a distância entre cada um dos nós de produto e a saída
        //as distâncias têm de ser introduzidas nos 2 sentidos.

        int numNos = task.getPicks().size() + 2;
        double[][] distanceMatrix = new double[numNos][numNos];
        List[][] rotas = new List[numNos][numNos];
        for (double[] row : distanceMatrix)
            Arrays.fill(row, 100000);

        Agent agent = task.getAgent();

        for (int i = 1; i < numNos - 1; i++){

            String nodeID1 = new Integer(task.getPicks().get(i - 1).getNode().getGraphNodeId()).toString();

            //Determina rota e custos entre o nó inicial e todos os nós de produto
            Route r = routeFinder.findRoute(
                    task.getGraph().getNode(agent.getStartNode()),
                    task.getGraph().getNode(nodeID1));
            distanceMatrix[0][i] = r.getCost();
            rotas[0][i] = r.getNodes();
            //Determina rota e custos entre cada nó de produto e o nó de saída
            r = routeFinder.findRoute(
                    task.getGraph().getNode(nodeID1),
                    task.getGraph().getNode(agent.getEndNode()));
            distanceMatrix[i][numNos - 1] = r.getCost();
            rotas[i][numNos - 1] = r.getNodes();
            for(int j = i + 1; j < numNos - 1; j++){
                if (i != j) {
                    //Determina rota e custos entre nós de produto usando o A*

                    String nodeID2 = new Integer(task.getPicks().get(j - 1).getNode().getGraphNodeId()).toString();

                    r = routeFinder.findRoute(
                            task.getGraph().getNode(nodeID1),
                            task.getGraph().getNode(nodeID2));
                    distanceMatrix[i][j] = r.getCost();
                    rotas[i][j] = r.getNodes();
                    distanceMatrix[j][i] = r.getCost();
                    rotas[j][i] = routeFinder.reverseRoute(r);
                }
            }
        }
        //Como o algoritmo TSP assume o regresso ao ponto de partida, define-se a distância entre a saída e a
        //entrada =0 (só nesse sentido) para não interferir com o cálculo do custo.
        //Rever possivelmente numa versão futura.
        distanceMatrix[numNos - 1][0] = 0;

        //Calcula a melhor rota entre a entrada e a saída, passando por todos os produtos
        TspDynamicProgrammingIterative solver = new TspDynamicProgrammingIterative(0, distanceMatrix);

        // Imprime o resultado do TSP
        // System.out.println("Tour: " + solver.getTour());

        //Junta as rotas numa lista única

        List<GraphNode> finalRoute = new ArrayList<>();
        List indices = solver.getTour();
        for (int i = 0; i < indices.size() - 1; i++){
            int o = (int) indices.get(i);
            int d = (int) indices.get(i + 1);

            if(rotas[o][d] != null) {
                if (!finalRoute.isEmpty())
                    finalRoute.remove(finalRoute.size() - 1);
                finalRoute.addAll(rotas[o][d]);
            }
        }

        Route route = new Route(finalRoute, solver.getTourCost());
        Solution solution = new Solution();
        solution.addRoute(agent, route);

        return solution;
    }


}
