package requestmanagers;

import arwstate.*;
import exceptions.WarehouseConfigurationException;
import orderpicking.AStarDistance;
import orderpicking.GNode;
import pathfinder.Graph;
import weka.clusterers.SimpleKMeans;
import weka.core.*;

import java.util.*;

/**
 * Policy:
 * - Picks are first split according to destinies;
 * - Picks with the same destiny are then split into clusters;
 * - Each cluster becomes a task;
 */
public class RequestsManagerAgentPerClustersPerDestiny extends RequestsManager {

    public RequestsManagerAgentPerClustersPerDestiny(WarehouseState warehouseState) {
        super(warehouseState);
    }

    /**
     * Loads the request received from the ERP in XML format
     * @param xmlERPRequest
     * @return
     */
    @Override
    public void addRequest(String xmlERPRequest) throws WarehouseConfigurationException {
        Request request = new Request();
        request.parseXMLERPRequest(xmlERPRequest);

        HashMap<String, List<Pick>> picksPerDestiny = new HashMap<>();
        for (Pick pick : request.getPicks()) {
            if (!warehouseState.getWarehouse().checkWms(pick.getDestiny()))
                throw new WarehouseConfigurationException("Destiny doesn't exist in warehouse xml!");

            if (!picksPerDestiny.containsKey(pick.getDestiny())) {
                picksPerDestiny.put(pick.getDestiny(), new LinkedList());
            }
            picksPerDestiny.get(pick.getDestiny()).add(pick);
        }

        for (String destiny : picksPerDestiny.keySet()) {
            buildTasksPerClusterPerDestiny(request, picksPerDestiny.get(destiny));
        }
    }

    private void buildTasksPerClusterPerDestiny(Request request, List<Pick> picks) {
        //We are assuming that all picks have the same destiny
        String destiny = picks.get(0).getDestiny();

        Graph<GNode> allPicksGraph = warehouseState.buildGraph(picks);

        int numClusters = Math.min(picks.size(), warehouseState.getMinNumAgents());

        while (picks.size() / numClusters < warehouseState.getMinAveragePicksPerTask()) {
            numClusters--;
        }

        while (picks.size() / numClusters > warehouseState.getMaxAveragePicksPerTask()) {
            numClusters++;
        }

        HashMap<Integer, List<Pick>> clusters = computeClusters(1, picks, allPicksGraph, numClusters);

        for (Integer clusterID : clusters.keySet()) {
            TaskOneAgentOneDestiny task = new TaskOneAgentOneDestiny(request, destiny);
            task.setPicks(clusters.get(clusterID));
            request.incNumberOfUnfinishedTasks();
            warehouseState.getPendingTasks().add(task);
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
