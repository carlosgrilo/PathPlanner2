package requestmanagers;

import arwdatastruct.Agent;
import arwdatastruct.TaskOneAgentOneDestiny;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

//A ver mais tarde: Nos dois request managers que usam DP não é preciso usar minNumAgent, a menos que introduzamos um
// mecanismo de sincronização, por exemplo, para evitar colisões.
public abstract class RequestsManagerDP extends RequestsManager{

    /**
     * handles pending tasks
     */
    @Override
    public Map<Agent, String> handlePendingTasks() {

        if (warehouse == null || arwGraph == null || arwGraph.getNumberOfNodes() == 0) {
            System.out.println("Armazem ou grafo não definido!");
            return null; //TODO Lançar exceção apropriada
        }

        //if (availableAgents.size() < minNumAgents || pendingTasks.size() < minNumAgents) {
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

        assignOneDestinyOneAgentTasks(tasksToBeProcessed, agents);

        Map<Agent, String> tasksAssignment = solveOneDestinyOneAgentTasks(tasksToBeProcessed);

        return tasksAssignment;
    }

}
