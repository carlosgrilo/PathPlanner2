package requestmanagers;

import arwstate.*;
import exceptions.WarehouseConfigurationException;

public class RequestsManagerOneTaskPerRequest extends RequestsManager{

    public RequestsManagerOneTaskPerRequest(WarehouseState warehouseState) {
        super(warehouseState);
    }

    @Override
    public void addRequest(String xmlERPRequest) throws WarehouseConfigurationException {
        Request request = new Request();
        request.parseXMLERPRequest(xmlERPRequest);

        Task task = new Task(request);

        for (Pick pick : request.getPicks()) {
            if (!warehouseState.getWarehouse().checkWms(pick.getDestiny())){
                throw new WarehouseConfigurationException("Destiny doesn't exist in warehouse xml!");
            }
            task.addPick(pick);
        }

        //NOTE: When using this manager, the created task is just a temporary task.
        // It is the solver that will decide how to split the pick into tasks.
        //So, we don't increment the number of unfinished tasks of the request
        //This should be done by the solver according to the number of tasks assigned,
        //request.incNumberOfUnfinishedTasks();
        warehouseState.getPendingTasks().add(task);
    }

}
