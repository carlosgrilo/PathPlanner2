package requestmanagers;

import arwstate.Agent;
import arwstate.WarehouseState;
import arwstate.Pick;
import arwstate.Request;
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
    public Request closeTask(String agentID, List<Pick> picks) throws WrongOperationException {
        if(!warehouseState.getOccupiedAgents().containsKey(agentID)){
            throw new WrongOperationException("ERROR: Agent was not occupied!");
        }
        Agent agent = warehouseState.getOccupiedAgents().get(agentID);
        Request request = agent.getTask().getRequest();
        request.decNumberTasksUnfinished();
        request.addSolvedPicks(picks);
        agent.setTask(null);
        return request.isSolved()? request : null;
    }

}
