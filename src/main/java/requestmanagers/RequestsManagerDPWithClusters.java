package requestmanagers;

import arwdatastruct.AStarDistance;
import arwdatastruct.Order;
import arwdatastruct.TaskOneAgentOneDestiny;
import orderpicking.GNode;
import orderpicking.Pick;
import orderpicking.Request;
import pathfinder.Graph;
import weka.clusterers.SimpleKMeans;
import weka.core.*;

import java.util.*;

/**
 *  Policy:
 *  - Picks are first split according to destinies;
 *  - Picks with the same destiny are then split in clusters;
 *  - Each cluster becomes a task;
 *  - Tasks are assigned to agents based on distance to cluster mass center;
 *  - Each task is solved using Dynamic Programming;
  */
public class RequestsManagerDPWithClusters extends RequestsManagerDP {

    /**
     * Loads the request received from the ERP in XML format
     *
     * @param xmlERPRequest
     * @return
     */
    //Para já, o método devolve false se houver algum destino não definido no armazém, e true em caso contrário...
    @Override
    public boolean addRequest(String xmlERPRequest) {
        Request request = new Request();
        request.parseXMLERPRequest(xmlERPRequest);

        HashMap<String, List<Pick>> picksPerDestiny = new HashMap<>();
        for (Order order : request.getOrders()) {
            for (Pick pick : order.getPicks()) {
                if (!warehouse.checkWms(pick.getDestiny()))
                    return false; //TODO Lançar exceção apropriada

                if (!picksPerDestiny.containsKey(pick.getDestiny())) {
                    picksPerDestiny.put(pick.getDestiny(), new LinkedList());
                }
                picksPerDestiny.get(pick.getDestiny()).add(pick);
            }
        }

        for (String destiny : picksPerDestiny.keySet()) {
            buildTasksPerClusterPerDestiny(request, picksPerDestiny.get(destiny));
        }

        return true;
    }

    private void buildTasksPerClusterPerDestiny(Request request, List<Pick> picks) {
        //We are assuming that all picks have the same destiny
        String destiny = picks.get(0).getDestiny();

        Graph<GNode> allPicksGraph = buildGraph(picks);

        int numClusters = Math.min(picks.size(), minNumAgents);

        while(picks.size() / numClusters < minAveragePicksPerTask){
            numClusters--;
        }

        while(picks.size() / numClusters > maxAveragePicksPerTask){
            numClusters++;
        }

        HashMap<Integer, List<Pick>> clusters = computeClusters(1, picks, allPicksGraph, numClusters);

        for (Integer clusterID : clusters.keySet()) {
            TaskOneAgentOneDestiny task = new TaskOneAgentOneDestiny(request, destiny);
            task.setPicks(clusters.get(clusterID));
            request.incNumberOfUnfinishedTasks();
            pendingTasks.add(task);
        }
    }

    private final int MAX_KMEANS_ITERATIONS = 20;

    private HashMap<Integer, List<Pick>> computeClusters(
            int seed,
            List<Pick> picks,
            Graph<GNode> graph,
            int numClusters) {
        try {
            SimpleKMeans kmeans = new SimpleKMeans();
            kmeans.setNumClusters(numClusters);
            kmeans.setMaxIterations(MAX_KMEANS_ITERATIONS);
            kmeans.setSeed(seed);
            kmeans.setPreserveInstancesOrder(true);
            DistanceFunction function = new AStarDistance(graph);
            kmeans.setDistanceFunction(function);

            // Define the feature list
            ArrayList<Attribute> attributes = new ArrayList<>();
            attributes.add(new Attribute("X"));
            attributes.add(new Attribute("Y"));
            attributes.add(new Attribute("nodeID"));
            Instances dataset = new Instances("All Picks", attributes, 0);

            for (Pick pick : picks) {
                double[] attValues = new double[dataset.numAttributes()];
                attValues[0] = pick.getNode().getX();
                attValues[1] = pick.getNode().getY();
                attValues[2] = pick.getNode().getGraphNodeId();
                Instance instance = new DenseInstance(1.0, attValues);
                dataset.add(instance);
            }

            kmeans.buildClusterer(dataset);
            HashMap<Integer, List<Pick>> clustersMap = new HashMap<>();
            for (int i = 0; i < numClusters; i++) {
                clustersMap.put(i, new ArrayList<>());
            }
            int numPicks = picks.size();
            for (int i = 0; i < numPicks; i++) {
                List<Pick> clusterPicks = clustersMap.get(kmeans.getAssignments()[i]);
                clusterPicks.add(picks.get(i));
            }
            return clustersMap;
        } catch (Exception ex) {
            System.err.println("Unable to build Clusterer: " + ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }

}

//            System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
//                    System.out.println("Task :" + clusterID);
//                    for (Pick pick : clusters.get(clusterID)) {
//                    System.out.println(pick.getOrigin());
//                    }
//            System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
