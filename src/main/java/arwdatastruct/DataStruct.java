package arwdatastruct;

import whdatastruct.PrefabManager;
import whdatastruct.Rack;
import whdatastruct.WHDataFuncs;
import whgraph.Graphs.ARWGraph;
import whgraph.Graphs.ARWGraphNode;
import orderpicking.GNode;
import orderpicking.Pick;
import orderpicking.PickingOrders;
import orderpicking.SingleOrderDyn;
import pathfinder.Graph;
import pathfinder.RouteFinder;

import java.util.*;

import static whgraph.Graphs.GraphNodeType.AGENT;
import static whgraph.Graphs.GraphNodeType.PRODUCT;

public class DataStruct {

    private PrefabManager warehouse;
    ARWGraph arwgrafo;
    ArrayList<Agent> availableagents;
    Hashtable<String, Agent> occupiedagents;
    ArrayList<Order> pendingorders;
    Hashtable<String, Order> assignedorders;
    String defaultdestiny="13.S.0.0";

    public DataStruct() {
        arwgrafo=new ARWGraph();
        availableagents=new ArrayList<>();
        occupiedagents=new Hashtable<>();
        pendingorders = new ArrayList<>();
        assignedorders = new Hashtable<>();
    }

    //O XML do armazem será sempre gravado após receção
    //O nome do ficheiro será guardado no interface
    public void setPrefabFromFile(String filename){
        warehouse = WHDataFuncs.readPrefabXML(filename);
    }

    public void setPrefab(PrefabManager prefabmanager){
        warehouse=prefabmanager;
    }

    //Função para atribuição do grafo, lido de ficheiro ou criado/editado com editor
    public void setGraph(ARWGraph arwgrafo){
        this.arwgrafo=arwgrafo;
    }

    public void setDefaultdestiny(String defaultdestiny) {
        this.defaultdestiny = defaultdestiny;
    }

    //Adiciona um novo agente quando ficar disponível
    public void newAgent(Agent agent){
        availableagents.add(agent);
    }

    //Retira um agente caso os óculos sejam desligados
    public void removeAgent(String agentid){
        for (Agent agent : availableagents)
            if (agent.id.equals(agentid)){
                availableagents.remove(agent);
                break;
            }

    }

    public Integer getAvailableAgents(){
        return new Integer(availableagents.size());
    }

    public Integer getPendingorders(){
        return new Integer(pendingorders.size());
    }


    public boolean checkagent(String agentid){
        return occupiedagents.containsKey(agentid);
    }

    public void releaseUpdate(String agentid, float newx, float newy){
        Agent agent=occupiedagents.get(agentid);
        agent.initialx=newx;
        agent.initialy=newy;
        availableagents.add(agent);
        occupiedagents.remove(agentid);
    }
    //Carrega uma tarefa recebida como XML
    public void addTask(String xmltask){
        PickingOrders pickingorders = new PickingOrders();

        pickingorders.parseTarefaXML(xmltask);
        pendingorders.addAll(pickingorders.getOrders());
    }

    //Trata tarefas pendentes
    public String HandleTasks(String assignedagent){
        if ((availableagents.size()>0)&&(pendingorders.size()>0)){
            //Para ver se resulta na obtenção da primeira da lista

            Order order=pendingorders.get(0);
            Hashtable<String, Pick> picks = order.lineorder;
            String keypick = (String) picks.keySet().toArray()[0];
            String destiny= picks.get(keypick).getDestiny();
            if (!warehouse.checkWms(destiny))
                destiny=defaultdestiny;
            Agent agent=availableagents.get(0);

            String xmlstring=PlanPath(picks, agent, destiny);
            assignedorders.put(agent.id,order);
            occupiedagents.put(agent.id,agent);
            pendingorders.remove(0);
            availableagents.remove(0);
            assignedagent= agent.id;
            return xmlstring;
        }
        else
            return "";
    }

    public String PlanPath(Hashtable<String, Pick> picks, Agent agent, String destiny)  {

        if ((warehouse==null)||(arwgrafo ==null)||(arwgrafo.getNumberOfgraphNodes()==0)){
            System.out.println("Armazem ou grafo não definido!");
            return "";
        }
        else {
            //Constroi grafo com todos os nós, incluindo produtos
            Graph<GNode> grafo;
            RouteFinder<GNode> routeFinder;

            ARWGraph problemgraph = arwgrafo.clone();

            Hashtable<Integer, ArrayList<Pick>> afetacao = new Hashtable();

            Set<String> pickkeys = picks.keySet();

            for (String pickkey : pickkeys) {
                Pick pick = picks.get(pickkey);
                Rack rack = warehouse.findRackBywmsCode(pick.getOrigin());
                int nnos = problemgraph.getNumNodes();
                nnos = problemgraph.insertNode(new ARWGraphNode(nnos, rack.getPosition().getX(), rack.getPosition().getY(), PRODUCT)).getGraphNodeId();
                if (afetacao.containsKey(nnos))
                    afetacao.get(nnos).add(pick);
                else {
                    ArrayList<Pick> listapicks = new ArrayList<Pick>();
                    listapicks.add(pick);
                    afetacao.put(nnos, listapicks);
                }
            }

            problemgraph.insertNode(new ARWGraphNode(problemgraph.getNumNodes(), agent.initialx, agent.initialy,
                    AGENT)).getGraphNodeId();
            //Os passos abaixo para a criação do vetor produtos são para simplificar. Serviram para aprendizagem.
            ArrayList<String> prods = new ArrayList<String>();
            for (int id : afetacao.keySet())
                prods.add(new Integer(id).toString());

            String[] produtos = prods.toArray(new String[prods.size()]);

            grafo = problemgraph.getPathGraph();

            //Determinar ponto de entrega
            //Colocar depois dentro do ciclo de leitura
            int endnode = arwgrafo.findClosestNode(warehouse.findRackBywmsCode(destiny).getPosition().getX(),
                    warehouse.findRackBywmsCode(destiny).getPosition().getY()).getGraphNodeId();

            ARWGraphNode startnode= problemgraph.findClosestNode(agent.initialx, agent.initialy);

            SingleOrderDyn orderDyn = new SingleOrderDyn(grafo, produtos, Integer.toString(startnode.getGraphNodeId()),
                    new Integer(endnode).toString());

            double cost = orderDyn.solve();

            Task tarefa = new Task(orderDyn.getRotafinal(), grafo, afetacao);

            String xmlstring = tarefa.XMLPath(agent.id, afetacao.keys().toString());
            System.out.println("Tour:\n " + xmlstring );

            // Print: 42.0
            System.out.println("Tour cost: " + cost);
            return xmlstring;

        }

    }

    public void concludeOrder(String orderid){
        assignedorders.remove(orderid);
    }

}