package requestmanagers;

import arwdatastruct.Order;
import arwdatastruct.TaskOneAgentOneDestiny;
import orderpicking.Pick;
import orderpicking.Request;
import java.util.*;

/**
 *  Policy:
 *  - For each request, pics are split into tasks per destiny per order;
 *  - Each task is solved using Dynamic Programming.
 */
public class RequestsManagerDPAgentPerDestinyPerOrder extends RequestsManagerDP {

    /**
     * Loads the request received from the ERP in XML format
     * @param xmlERPRequest
     * @return
     */
    //Para já, o método devolve false se houver algum destino não definido no armazém, e true em caso contrário...
    @Override
    public boolean addRequest(String xmlERPRequest) {
        Request request = new Request();
        request.parseXMLERPRequest(xmlERPRequest);

        for (Order order : request.getOrders()) {

            List<Pick> orderPicks = new ArrayList<>(order.getPicks());

            Map<String, TaskOneAgentOneDestiny> tasksPerOrderDestiny = new HashMap<>();

            for (Pick pick : orderPicks) {
                String destiny = pick.getDestiny();
                if (!warehouse.checkWms(destiny))
                    return false; //TODO Lançar exceção apropriada
                TaskOneAgentOneDestiny task = tasksPerOrderDestiny.get(pick.getDestiny());
                if (task == null) {
                    task = new TaskOneAgentOneDestiny(request, destiny);
                    request.incNumberOfUnfinishedTasks();
                    tasksPerOrderDestiny.put(pick.getDestiny(), task);
                }
                task.addPick(pick);
            }
            pendingTasks.addAll(new LinkedList(tasksPerOrderDestiny.values()));
        }

        return true;
    }
}
