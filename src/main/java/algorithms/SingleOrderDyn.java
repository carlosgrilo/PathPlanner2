package algorithms;

import arwdatastruct.Agent;
import orderpicking.DistanceScorer;
import pathfinder.*;
import tsp.TspDynamicProgrammingIterative;

import java.util.*;

//This algorithm assumes that that there is only one agent available
public class SingleOrderDyn{

    public Solution solve(Problem problem){

        RouteFinder routeFinder = new RouteFinder<>(problem.getGraph(), new DistanceScorer(), new DistanceScorer());
        //Constroi matriz de distâncias com m x m, m = nnosproduto + entrada+saída
        //a 1ª linha serve para a distância entre o nó de partida e cada um dos nós de produto
        //a última linha tem a distância entre cada um dos nós de produto e a saída
        //as distâncias têm de ser introduzidas nos 2 sentidos.

        int numNos = problem.getPicks().size() + 2;
        double[][] distanceMatrix = new double[numNos][numNos];
        List[][] rotas = new List[numNos][numNos];
        for (double[] row : distanceMatrix)
            Arrays.fill(row, 100000);

        Agent agent = problem.getAgents().get(0);

        for (int i = 1; i < numNos - 1; i++){

            String nodeID1 = new Integer(problem.getPicks().get(i - 1).getNode().getGraphNodeId()).toString();

            //Determina rota e custos entre o nó inicial e todos os nós de produto
            Route r = routeFinder.findRoute(
                    problem.getGraph().getNode(agent.getStartNode()),
                    problem.getGraph().getNode(nodeID1));
            distanceMatrix[0][i] = r.getCost();
            rotas[0][i] = r.getRoute();
            //Determina rota e custos entre cada nó de produto e o nó de saída
            r = routeFinder.findRoute(
                    problem.getGraph().getNode(nodeID1),
                    problem.getGraph().getNode(agent.getEndNode()));
            distanceMatrix[i][numNos-1] = r.getCost();
            rotas[i][numNos - 1] = r.getRoute();
            for(int j = i + 1; j < numNos - 1; j++){
                if (i != j) {
                    //Determina rota e custos entre nós de produto usando o A*

                    String nodeID2 = new Integer(problem.getPicks().get(j - 1).getNode().getGraphNodeId()).toString();

                    r = routeFinder.findRoute(
                            problem.getGraph().getNode(nodeID1),
                            problem.getGraph().getNode(nodeID2));
                    distanceMatrix[i][j] = r.getCost();
                    rotas[i][j] = r.getRoute();
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
        TspDynamicProgrammingIterative solver =
                new TspDynamicProgrammingIterative(0, distanceMatrix);

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
