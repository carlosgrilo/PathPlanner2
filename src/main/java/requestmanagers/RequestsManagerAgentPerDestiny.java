package requestmanagers;

import arwstate.WarehouseState;
import arwstate.TaskOneAgentOneDestiny;
import arwstate.Pick;
import arwstate.Request;
import exceptions.WarehouseConfigurationException;

import java.util.*;

/**
 *  Policy: For each request, pics are split into tasks per destiny;
 */
public class RequestsManagerAgentPerDestiny extends RequestsManager{

    public RequestsManagerAgentPerDestiny(WarehouseState warehouseState) {
        super(warehouseState);
    }

    /**
     * Loads the request received from the ERP in XML format
     * @param xmlERPRequest
     * @return
     */
    public void addRequest(String xmlERPRequest) throws WarehouseConfigurationException {
        Request request = new Request();
        request.parseXMLERPRequest(xmlERPRequest);

        Map<String, TaskOneAgentOneDestiny> tasksPerDestiny = new HashMap<>();

        for (Pick pick : request.getPicks()) {
            String destiny = pick.getDestiny();
            if (!warehouseState.getWarehouse().checkWms(destiny))
                throw new WarehouseConfigurationException("Destiny doesn't exist in warehouse xml!");
            TaskOneAgentOneDestiny task = tasksPerDestiny.get(pick.getDestiny());
            if (task == null) {
                task = new TaskOneAgentOneDestiny(request, destiny);
                request.incNumberOfUnfinishedTasks();
                tasksPerDestiny.put(pick.getDestiny(), task);
            }
            task.addPick(pick);
        }
        warehouseState.getPendingTasks().addAll(new LinkedList(tasksPerDestiny.values()));
    }

}
