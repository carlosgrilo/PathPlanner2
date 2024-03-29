package requestmanagers;

import arwstate.*;
import exceptions.WarehouseConfigurationException;
import exceptions.WrongOperationException;

import java.util.*;

public abstract class RequestsManager {

    protected WarehouseState warehouseState;

    public RequestsManager(WarehouseState warehouseState) {
        this.warehouseState = warehouseState;
    }

    /**
     * Loads the request received from the ERP in XML format, splits it into tasks and add them to the
     * pendingTasks list.
     * @param xmlERPRequest
     * @return
     */
    public abstract void addRequest(String xmlERPRequest) throws WarehouseConfigurationException;

    /**
     * If it is the last class from a request, returns the request
     * If not, returns null;
     * The agent is "released" at TOPIC_OPAVAIL in PathPlanner using method releaseAgent()
     * @param agentID
     * @param picks
     * @return
     */
    public String closeTask(String agentID, List<Pick> picks) throws WrongOperationException {
        if(!warehouseState.getOccupiedAgents().containsKey(agentID)){
            return null; // throw new WrongOperationException("ERROR: Agent was not occupied!");
        }
        Agent agent = warehouseState.getOccupiedAgents().get(agentID);
        Request request = agent.getTask().getRequest();
        request.decNumberTasksUnfinished();
        request.addSolvedPicks(picks);
        agent.setTask(null);
        return request.isSolved()? request.concludedPicksToXML() : null;
    }

    public Task handleIncompleteTask(String agentID, List<Pick> picks) throws WrongOperationException {
        if(!warehouseState.getOccupiedAgents().containsKey(agentID)){
            return null; // throw new WrongOperationException("ERROR: Agent was not occupied!");
        }
        Agent agent = warehouseState.getOccupiedAgents().get(agentID);
        Task task=agent.getTask();
        Request request = task.getRequest();
        //request.decNumberTasksUnfinished();
        request.addSolvedPicks(picks);
        task.removePicks(picks);
        agent.setTask(null);
        return task;
    }

    /**
     * If it is the last class from a request, returns the request
     * If not, returns null;
     * The agent is "released" at TOPIC_OPAVAIL in PathPlanner using method releaseAgent()
     * @param agentID
     * @return
     */
    public String closeCancelledTask(String agentID) throws WrongOperationException {
        if(!warehouseState.getOccupiedAgents().containsKey(agentID)){
            return null; // throw new WrongOperationException("ERROR: Agent was not occupied!");
        }
        Agent agent = warehouseState.getOccupiedAgents().get(agentID);
        Request request = agent.getTask().getRequest();
        request.decNumberTasksUnfinished();
        agent.setTask(null);
        return request.isSolved()? request.concludedPicksToXML() : null;
    }


}
