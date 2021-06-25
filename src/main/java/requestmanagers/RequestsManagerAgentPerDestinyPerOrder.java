package requestmanagers;

import arwstate.WarehouseState;
import arwstate.TaskOneAgentOneDestiny;
import arwstate.Pick;
import arwstate.Request;
import exceptions.WarehouseConfigurationException;

import java.util.*;

/**
 *  Policy: For each request, pics are split into tasks per destiny per order;
 */
public class RequestsManagerAgentPerDestinyPerOrder extends RequestsManager {

    public RequestsManagerAgentPerDestinyPerOrder(WarehouseState warehouseState) {
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

        Map<String, List<Pick>> picksPerOrder = getPicksPerOrder(request.getPicks());

        for (List<Pick> orderPicks: picksPerOrder.values()){

            Map<String, TaskOneAgentOneDestiny> tasksPerDestinyPerOrder = new HashMap<>();

            for (Pick pick : orderPicks) {
                String destiny = pick.getDestiny();
                if (!warehouseState.getWarehouse().checkWms(destiny))
                    throw new WarehouseConfigurationException("Destiny doesn't exist in warehouse xml!");
                TaskOneAgentOneDestiny task = tasksPerDestinyPerOrder.get(pick.getDestiny());
                if (task == null) {
                    task = new TaskOneAgentOneDestiny(request, destiny);
                    request.incNumberOfUnfinishedTasks();
                    tasksPerDestinyPerOrder.put(pick.getDestiny(), task);
                }
                task.addPick(pick);
            }
            warehouseState.getPendingTasks().addAll(new LinkedList(tasksPerDestinyPerOrder.values()));
        }
    }

    private Map<String, List<Pick>> getPicksPerOrder(List<Pick> picks){
        Map<String, List<Pick>> picksPerOrder = new HashMap<>();
        for (Pick pick : picks) {
            String orderID = pick.getOrderID();
            if(!picksPerOrder.containsKey(orderID)){
                picksPerOrder.put(orderID, new LinkedList<>());
            }
            List<Pick> orderPicks = picksPerOrder.get(orderID);
            orderPicks.add(pick);
        }
        return picksPerOrder;
    }
}
