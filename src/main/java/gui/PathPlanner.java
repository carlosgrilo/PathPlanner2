package gui;


import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.nio.file.Files;

import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import arwstate.*;

import exceptions.WarehouseConfigurationException;
import exceptions.WrongOperationException;
import newWarehouse.Warehouse;

import gui.utils.*;
import net.sf.jni4net.Bridge;


import arwstate.Pick;
import arwstate.Request;
import org.json.JSONObject;

import classlib.BusMessage;
import classlib.CommunicationManager;
import classlib.TopicsConfiguration;
import classlib.Util;
import communication.esb_callbacks;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import requestmanagers.*;
import solvers.Solver;
import solvers.SolverDynamicProgramming;
import whgraph.ARWGraph;

import static xmlutils.XMLfuncs.read_xml_from_file;
import static xmlutils.XMLfuncs.write_xml_to_file;


public class PathPlanner extends JFrame {

    public static PrintStream printOut;
    public static String CLIENT_ID = "planeador";
    public static final int DEFAULT_MIN_NUM_AGENTS = 2;
    public static final int DEFAULT_MIN_AVERAGE_PICKS_PER_TASK = 2;
    public static final int DEFAULT_MAX_AVERAGE_PICKS_PER_TASK = 10;
    private static float corridorWidth = 1f;
    public static final String ERP_ID = "ERP";
    public static final String RA_ID = "ra1";
    public static final String LOC_APROX_ID = "locaproximada";
    public static final String MODELADOR_ID = "modelador";
    public static final String WAREHOUSE_FILE = "warehouse_model.xml";
    public static final String GRAPH_FILE = "graph_mat_ceramica_2021_06_30.xml";
    public static final String TOPIC_UPDATEXML = "mod_updateXML";
    public static final String TOPIC_ACKXML = "mod_updateXMLstatus";
    public static final String TOPIC_OPAVAIL = "available";
    public static final String TOPIC_NEWOP = "newOperator";
    public static final String TOPIC_GETTASK = "getTarefa";
    public static final String TOPIC_ENDTASK = "endTask";
    public static final String TOPIC_NEWTASK = "newTask";
    public static final String TOPIC_CONCLUDETASK = "setTarefaFinalizada";

    private final BackgroundSurface background;
    private final GraphSurface graphsurface;

    private final JTextArea consola;
    private final JTextField numTasks;
    private final JTextField numOps;
    private final JLabel alertaNovoXML;
    private final Warehouse warehouse;
    private final ARWGraph arwgraph;
    Timer timer, timeXML;
    String idXML = "";
    Boolean xmlReceived;
    PacManSurface pacmanSurface;
    CommunicationManager cm;
    esb_callbacks Checkbus;
    String lastTask;
    private WarehouseState warehouseState;
    private RequestsManager requestsManager;
    private Solver solver;

    public int erpCheckPeriod = 5; //MINUTOS
    private boolean checkERPTasksAutomatically;
    CheckERP erpRequestTask;

    public PathPlanner() {
        super("ARWARE Path Planner v2beta");

        checkERPTasksAutomatically = true;
        erpRequestTask = new CheckERP();
        setLayout(new BorderLayout());

        setupMenuBar();

        this.warehouse = new Warehouse();
        try {
            String xmlcontent = read_xml_from_file(WAREHOUSE_FILE);
            warehouse.createFromXML(xmlcontent);
        } catch (IOException e) {
            e.printStackTrace();
        }

        arwgraph = new ARWGraph();
        int depth=Toolkit.getDefaultToolkit().getScreenSize().height*90/100;
        int width=depth;
        if (warehouse.getWidth()>warehouse.getDepth())
            depth= Math.round(width*warehouse.getDepth()/warehouse.getWidth());
        else
            width=Math.round( depth*warehouse.getWidth()/ warehouse.getDepth());

        //setSize(width+100, depth);
        background = new BackgroundSurface(warehouse, false, width);

        File file = new File(GRAPH_FILE);
        //VERIFICAR COMO TESTAR PARA ERRO
        arwgraph.readGraphFile(file);

        warehouseState = new WarehouseState(
                warehouse,
                arwgraph,
                DEFAULT_MIN_NUM_AGENTS,
                DEFAULT_MIN_AVERAGE_PICKS_PER_TASK,
                DEFAULT_MAX_AVERAGE_PICKS_PER_TASK);

        requestsManager = new RequestsManagerAgentPerDestinyPerOrder(warehouseState);
        solver = new SolverDynamicProgramming(warehouseState);

        graphsurface = new GraphSurface(arwgraph, warehouse, 5);
        pacmanSurface = new PacManSurface(background, 15);
        JLayer<JPanel> jLayer = new JLayer<>(background, graphsurface);

        JPanel pane = new JPanel();
        pane.setLayout(new BorderLayout());
        pane.add(pacmanSurface, BorderLayout.CENTER);

        jLayer.setGlassPane(pane);
        pane.setOpaque(false);
        pane.setVisible(true);

        consola = new JTextArea(5, 40);
        JScrollPane scrollpane = new JScrollPane(consola);

        JLabel et_tasks = new JLabel("Tarefas pendentes");
        JLabel et_ops = new JLabel("Operadores disponíveis");
        numTasks = new JTextField("0");
        numOps = new JTextField("0");
        numTasks.setEditable(false);
        numOps.setEditable(false);
        alertaNovoXML = new JLabel("");

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.add(et_tasks);
        panel.add(numTasks);
        panel.add(new JLabel("      "));
        panel.add(et_ops);
        panel.add(numOps);
        panel.add(alertaNovoXML);

        add(panel, BorderLayout.NORTH);
        //add(pacmansurface,BorderLayout.CENTER);
        add(jLayer, BorderLayout.CENTER);

        add(scrollpane, BorderLayout.PAGE_END);

        //setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                printOut.flush();
                printOut.close();
                System.exit(0);
            }
        });
        pack();
        setSize(width+50, depth);
        initComponents();
        xmlReceived = false;
        setVisible(true);
    }

    private void avisaNovoXML() {
        alertaNovoXML.setText("Novo Modelo de armazem disponível");
        alertaNovoXML.setForeground(Color.red);
    }

    private void retiravisoXML() {
        alertaNovoXML.setText("");
        alertaNovoXML.setForeground(Color.gray);
    }

    private void setupMenuBar() {
        JMenu menu;
        JMenuItem menuItem;
        JMenuBar menubar = new JMenuBar();

        //Build the first menu.
        menu = new JMenu("Data");
        menu.setMnemonic(KeyEvent.VK_D);
        menu.getAccessibleContext().setAccessibleDescription("File options");
        menubar.add(menu);

        //a group of JMenuItems
        menuItem = new JMenuItem("Load Warehouse", KeyEvent.VK_W);
        menuItem.getAccessibleContext().setAccessibleDescription("Load Warehouse Model");
        menuItem.addActionListener(e -> loadWarehouse());
        menu.add(menuItem);

        menuItem = new JMenuItem("Generate graph", KeyEvent.VK_G);
        menuItem.getAccessibleContext().setAccessibleDescription("Generate new graph of path nodes");
        menuItem.addActionListener(e -> autoGraph());
        menu.add(menuItem);

        menuItem = new JMenuItem("Load graph", KeyEvent.VK_L);
        menuItem.getAccessibleContext().setAccessibleDescription("Load graph of path nodes");
        menuItem.addActionListener(e -> loadGraph());
        menu.add(menuItem);

        menuItem = new JMenuItem("Edit graph", KeyEvent.VK_E);
        menuItem.getAccessibleContext().setAccessibleDescription("Edit graph of path nodes");
        menuItem.addActionListener(e -> editGraph());
        menu.add(menuItem);

        menuItem = new JMenuItem("Ask ERP task", KeyEvent.VK_S);
        menuItem.getAccessibleContext().setAccessibleDescription("Ask ERP for a Task");
        menuItem.addActionListener(e -> askERPTask());
        menu.add(menuItem);

        menuItem = new JMenuItem("Load Tasksim", KeyEvent.VK_S);
        menuItem.getAccessibleContext().setAccessibleDescription("Load Simulated Task");
        menuItem.addActionListener(e -> loadTasksim());
        menu.add(menuItem);

        //Build the Requests Managers menu.
        menu = new JMenu("Requests Managers");
        menu.setMnemonic(KeyEvent.VK_R);
        menu.getAccessibleContext().setAccessibleDescription("Requests Managers");
        menubar.add(menu);

        //a group of JMenuItems
        menuItem = new JMenuItem("Agent per Destiny", KeyEvent.VK_D);
        menuItem.getAccessibleContext().setAccessibleDescription("Agent per Destiny");
        menu.add(menuItem);
        menuItem.addActionListener(e -> setRequestManagerAgentPerDestiny());

        menuItem = new JMenuItem("Agent per Destiny per Order", KeyEvent.VK_O);
        menuItem.getAccessibleContext().setAccessibleDescription("Agent per Destiny per Order");
        menu.add(menuItem);
        menuItem.addActionListener(e -> setRequestManagerAgentPerDestinyPerOrder());

        menuItem = new JMenuItem("Agent per Cluster per Destiny", KeyEvent.VK_C);
        menuItem.getAccessibleContext().setAccessibleDescription("Agent per Cluster per Destiny");
        menuItem.addActionListener(e -> setRequestManagerAgentPerClustersPerDestiny());
        menu.add(menuItem);

        menuItem = new JMenuItem("One Task per Request", KeyEvent.VK_R);
        menuItem.getAccessibleContext().setAccessibleDescription("One Task per Request");
        menuItem.addActionListener(e -> setRequestManagerOneTaskPerRequest());
        menu.add(menuItem);

        //Build the Solvers menu.
        menu = new JMenu("Solvers");
        menu.setMnemonic(KeyEvent.VK_S);
        menu.getAccessibleContext().setAccessibleDescription("Solvers");
        menubar.add(menu);

        //a group of JMenuItems
        menuItem = new JMenuItem("Dynamic Programming", KeyEvent.VK_D);
        menuItem.getAccessibleContext().setAccessibleDescription("Dynamic Programming");
        menu.add(menuItem);
        menuItem.addActionListener(e -> setSolverDynamicProgramming());

        //Build the settings menu.
        menuItem = new JMenuItem("Settings");
        menuItem.setMnemonic(KeyEvent.VK_S);
        menuItem.getAccessibleContext().setAccessibleDescription("Settings");
        menubar.add(menuItem);
        menuItem.addActionListener(e -> openSettings());

        this.setJMenuBar(menubar);
    }

    private void autoGraph() {
        arwgraph.createGraph(warehouse, corridorWidth);
        repaint();
    }

    private void updateData() {
        numTasks.setText(warehouseState.getNumberOfPendingTasks().toString());
        numOps.setText(warehouseState.getNumberOfAvailableAgents().toString());
        repaint();
    }

    private void editGraph() {
        GraphEditor frame = new GraphEditor(warehouse, arwgraph, corridorWidth);
        repaint();
    }

    private void setRequestManagerAgentPerDestiny() {
        requestsManager = new RequestsManagerAgentPerDestiny(warehouseState);
    }

    private void setRequestManagerAgentPerDestinyPerOrder() {
        requestsManager = new RequestsManagerAgentPerDestinyPerOrder(warehouseState);
    }

    private void setRequestManagerAgentPerClustersPerDestiny() {
        requestsManager = new RequestsManagerAgentPerClustersPerDestiny(warehouseState);
    }

    private void setRequestManagerOneTaskPerRequest() {
        requestsManager = new RequestsManagerOneTaskPerRequest(warehouseState);
    }

    private void setSolverDynamicProgramming() {
        solver = new SolverDynamicProgramming(warehouseState);
    }

    public void openSettings() {
        System.out.println("Aviso de diálogo de settings");
        SettingsDialog settingsDialog = new SettingsDialog(
                this,
                erpCheckPeriod,
                corridorWidth,
                CLIENT_ID,
                warehouseState.getMinNumAgents(),
                warehouseState.getMinAveragePicksPerTask(),
                warehouseState.getMaxAveragePicksPerTask());

        if(!settingsDialog.cancel) {
            if (settingsDialog.toggleButton.isSelected() && checkERPTasksAutomatically == false ||
                    erpCheckPeriod != settingsDialog.erpCheckPeriod && settingsDialog.toggleButton.isSelected()) {
                checkERPTasksAutomatically = true;
                erpCheckPeriod = settingsDialog.erpCheckPeriod;
                erpRequestTask.cancel();
                erpRequestTask = new CheckERP();
                timer.schedule(erpRequestTask, 0, TimeUnit.MINUTES.toMillis(erpCheckPeriod));
            } else if (!settingsDialog.toggleButton.isSelected() && checkERPTasksAutomatically == true) {
                checkERPTasksAutomatically = false;
            }
        }

        corridorWidth = settingsDialog.corridorWidth;
        CLIENT_ID = settingsDialog.clientID;
        checkERPTasksAutomatically = settingsDialog.toggleButton.isSelected();

        warehouseState.setMinNumAgents(settingsDialog.minNumberAgents);
        warehouseState.setMinAveragePicksPerTask(settingsDialog.minAveragePicksPerTask);
        warehouseState.setMaxAveragePicksPerTask(settingsDialog.maxAveragePicksPerTask);

        repaint();
    }

    public void loadTasksim() {
        //Carrega uma tarefa pré-gravada, para testes
        String xmlstring;
        try {
            xmlstring = read_xml_from_file("tarefa.xml");
            handleRequest(xmlstring);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void loadWarehouse() {
        JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
        fc.setSelectedFile(new File(WAREHOUSE_FILE));
        FileNameExtensionFilter filter = new FileNameExtensionFilter(WAREHOUSE_FILE, "xml");
        fc.setFileFilter(filter);
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                String xmlcontent = read_xml_from_file(fc.getSelectedFile().getName());
                warehouse.createFromXML(xmlcontent);
                arwgraph.createGraph(warehouse, corridorWidth);
                File source = new File(fc.getSelectedFile().getName());
                File dest = new File(WAREHOUSE_FILE);

                Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
            graphsurface.setPrefabManager(warehouse);
            background.repaint();
            retiravisoXML();
            repaint();
        }
    }

    private void loadGraph() {
        JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
        fc.setSelectedFile(new File(GRAPH_FILE));
        FileNameExtensionFilter filter = new FileNameExtensionFilter(GRAPH_FILE, "xml");
        fc.setFileFilter(filter);
        int returnVal = fc.showOpenDialog(this);
        try {
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                arwgraph.readGraphFile(file);
                repaint();
            }
        } catch (NoSuchElementException e2) {
            JOptionPane.showMessageDialog(this, "File format not valid", "Error!",
                    JOptionPane.ERROR_MESSAGE);
        }
        repaint();
    }

    private void initComponents() {

        Checkbus = new esb_callbacks();
        //dados = new DataStruct();

        cm = new CommunicationManager(CLIENT_ID, new TopicsConfiguration(), Checkbus);
        Checkbus.SetCommunicationManager(cm); // Está imbricado. Tentar ver se é possível alterar!
        Checkbus.addPropertyChangeListener(new PropertyChangeListener() {
                                               @Override
                                               public void propertyChange(PropertyChangeEvent evt) {
                                                   handleMessages((BusMessage) evt.getNewValue());
                                               }
                                           }
        );

        cm.SubscribeContentAsync(TOPIC_UPDATEXML, MODELADOR_ID);

        System.out.println("CLIENT_ID: " + CLIENT_ID);
        System.out.println("ERP_ID: " + ERP_ID);
        System.out.println("RA_ID: " + RA_ID + "[MAC]");
        System.out.println("LOC_APROX_ID: " + LOC_APROX_ID);
        System.out.println("MODELADOR_ID: " + MODELADOR_ID);

        timer = new Timer(); // Instantiate Timer Object
        timer.schedule(erpRequestTask, 0, TimeUnit.MINUTES.toMillis(erpCheckPeriod));
    }

    public void askERPTask() {
        if (arwgraph.getNumberOfNodes() > 0) {
            this.consola.append("Pedida tarefa" + '\n');
            this.cm.SendMessageAsync(Util.GenerateId(), "request", TOPIC_GETTASK, ERP_ID, "PlainText", "Dá-me uma tarefa!", "1");
        }
    }

    public void sendTask(String agentID, String xmlString) {
        try {
            playPacman(agentID, xmlString);
        } catch (IOException | SAXException e) {
            e.printStackTrace();
        }
        this.consola.append("A enviar tarefa para " + agentID + "\n");
        cm.SendMessageAsync(Util.GenerateId(), "request", TOPIC_NEWTASK, agentID, "application/xml", xmlString, "1");

    }

    public void finishTask(String xmlString) {
        this.consola.append("Enviada a conclusão de tarefa\n");
        cm.SendMessageAsync(
                Util.GenerateId(),
                "response",
                TOPIC_CONCLUDETASK,
                ERP_ID,
                "application/xml",
                xmlString,
                "1");
    }

    public void handleMessages(BusMessage busMessage) {

        String xmlStr;
        Float[] position = {(float) 0.0, (float) 0.0};

        switch (busMessage.getMessageType()) {

            case "request":
                System.out.println("REQUEST message ready to be processed.");
                String identificador = busMessage.getInfoIdentifier();

                switch (identificador) {

                    case TOPIC_OPAVAIL:
                        xmlStr = busMessage.getContent();
                        System.out.println(xmlStr);//Provisoriamente para teste
                        consola.append(xmlStr + "\n");
                        String[] split = busMessage.getFromTopic().split("Topic");
                        //Identifica o agente
                        String agentID = split[0];

                        JSONObject obj = new JSONObject(xmlStr);

                        //Vai buscar a posição do agente
                        if (obj.has("posicaox"))
                            position[0] = Float.parseFloat(obj.get("posicaox").toString());
                        if (obj.has("posicaoy"))
                            position[1] = Float.parseFloat(obj.get("posicaoy").toString());

                        //Se não houver warehouse ou posição do agente inválida
                        if (warehouse == null || position[0] > warehouse.getWidth() || position[1] > warehouse.getDepth()) {
                            cm.SendMessageAsync((Integer.parseInt(busMessage.getId()) + 1) + "", "response",
                                    busMessage.getInfoIdentifier(), split[0], "application/json", "{'response':'not ACK'}", "1");
                            //Faz sentido a linha abaixo?
                            pacmanSurface.addAgent(agentID, new Point2D.Float(position[0], position[1]));

                            //return?? Como houve problemas, devia saltar fora...
                        }

                        if (warehouseState.checkOccupiedAgent(agentID)) {
                            Agent agent = warehouseState.getOccupiedAgents().get(agentID);
                            if(agent.getTask() == null){
                                warehouseState.releaseAgent(agentID, position[0], position[1]);
                            }else{
                                try{
                                    xmlStr = requestsManager.closeCancelledTask(agentID);
                                    warehouseState.releaseAgent(agentID, position[0], position[1]);

                                    if (xmlStr != null) {
                                        write_xml_to_file("tarefa_concluida.xml", xmlStr);
                                        finishTask(xmlStr);
                                    }

                                    System.out.println("Concluded cancelled task");

                                    cm.SendMessageAsync(
                                            (Integer.parseInt(busMessage.getId()) + 1) + "",
                                            "response",
                                            busMessage.getInfoIdentifier(),
                                            split[0],
                                            "application/json",
                                            "{'response':'ACK'}",
                                            "1");
                                    System.out.println("Enviou Ack!");

                                    //NOTA: Este código pode ter que mudar para uma outra nova opção do switch
                                    //porque uma possibilidade para tratar este caso é o planner pedir uma
                                    //confirmação ao agente e, depois, quando ele confirma (nova opção do switch)
                                    // nós fechamos a tarefa que foi cancelada.

                                }catch (WrongOperationException e){
                                    e.printStackTrace();
                                }catch (IOException e){
                                    e.printStackTrace();
                                }
                            }

                        } else {
                            try {
                                warehouseState.addAvailableAgent(new Agent(agentID, position[0], position[1]));
                            } catch (WrongOperationException e){
                                e.printStackTrace();
                            }
                        }

                        cm.SendMessageAsync(
                                (Integer.parseInt(busMessage.getId()) + 1) + "",
                                "response",
                                busMessage.getInfoIdentifier(),
                                split[0],
                                "application/json",
                                "{'response':'ACK'}",
                                "1");

                        Map<Agent, String> tasks = solver.solve();

                        if (tasks != null) {
                            for (Agent agent : tasks.keySet()) {
                                sendTask(agent.getId(), tasks.get(agent));
                            }
                        } else{
                            if(checkERPTasksAutomatically){
                                askERPTask();
                            }
                        }

                        break;

                    case TOPIC_NEWOP:

                        xmlStr = busMessage.getContent();
                        System.out.println(xmlStr);//Provisoriamente para teste
                        consola.setText(xmlStr + "\n");
                        split = busMessage.getFromTopic().split("Topic");
                        agentID = split[0];

//                        try {
                            //VER COMO RESPONDER NESTE CASO. A SOLUÇÃO ATUAL É TEMPORÁRIA
                            //String xml_armazem = read_xml_from_file(WAREHOUSE_FILE);
                            String jsonAnswer = new JSONObject()
                                    .put("ack", "OK").toString();
                            cm.SendMessageAsync(
                                    (Integer.parseInt(busMessage.getId()) + 1) + "",
                                    "response",
                                    busMessage.getInfoIdentifier(),
                                    agentID,
                                    "application/xml",
                                    jsonAnswer,
                                    "1");
//                        } catch (IOException e) {
//                            cm.SendMessageAsync(
//                                    (Integer.parseInt(busMessage.getId()) + 1) + "",
//                                    "response",
//                                    busMessage.getInfoIdentifier(),
//                                    agentID,
//                                    "application/json",
//                                    "{'response':'ACK'}",
//                                    "1");
//                        }

                        break;

                    case TOPIC_ENDTASK:
                        xmlStr = busMessage.getContent();
                        System.out.println(xmlStr);//Provisoriamente para teste
                        consola.setText("Operador concluiu tarefa\n");
                        split = busMessage.getFromTopic().split("Topic");
                        //Identifica o agente
                        agentID = split[0];
                        try {
                            List<Pick> solvedPicks = Task.parseXMLConcludedTask(xmlStr);

                            if (warehouseState.checkOccupiedAgent(agentID)) {
                                Agent agent = warehouseState.getOccupiedAgents().get(agentID);
                                if (agent.getTask() == null) {
                                    cm.SendMessageAsync(
                                            (Integer.parseInt(busMessage.getId()) + 1) + "",
                                            "response",
                                            busMessage.getInfoIdentifier(),
                                            split[0],
                                            "application/json",
                                            "{'response':'ACK'}",
                                            "1");
                                    System.out.println("Enviou Ack!");
                                    break;
                                }
                            }

                            xmlStr = requestsManager.closeTask(agentID, solvedPicks);

                            if (xmlStr != null) {
                                write_xml_to_file("tarefa_concluida.xml", xmlStr);
                                finishTask(xmlStr);
                                System.out.println("Succesfully saved concluded task");
                            }

                            cm.SendMessageAsync(
                                    (Integer.parseInt(busMessage.getId()) + 1) + "",
                                    "response",
                                    busMessage.getInfoIdentifier(),
                                    split[0],
                                    "application/json",
                                    "{'response':'ACK'}",
                                    "1");
                            System.out.println("Enviou Ack!");

                        } catch (IOException e) {
                            System.out.println("Error while saving concluded task");
                            System.out.println(e.getMessage());
                        } catch(WrongOperationException e){
                            e.printStackTrace();
                        }

                        //GRILO: QUE MENSAGEM ENVIAMOS EM CASO DE ERRO, EM PARTICULAR, SE O AGENTE DIZ QUE
                        //TERMINOU A TAREFA DUAS VEZES SEGUIDAS?

                        break;

                    case TOPIC_ACKXML:
                        split = busMessage.getFromTopic().split("Topic");
                        if (xmlReceived) {
                            String jsonAnswer1 = new JSONObject()
                                    .put("id", idXML)
                                    .put("ack", "OK").toString();

                            cm.SendMessageAsync(
                                    Util.GenerateId(),
                                    "response",
                                    busMessage.getInfoIdentifier(),
                                    split[0],
                                    "application/json",
                                    jsonAnswer1,
                                    "1");
                            xmlReceived = false;
                        } else {
                            String jsonAnswer2 = new JSONObject()
                                    .put("id", idXML)
                                    .put("ack", "ERROR").toString();
                            cm.SendMessageAsync(
                                    Util.GenerateId(),
                                    "response",
                                    TOPIC_ACKXML,
                                    MODELADOR_ID,
                                    "application/json",
                                    jsonAnswer2,
                                    "1");
                            consola.append("Erro com a receção de XML");
                            System.out.println("Houve Erro com a receção de XML!");
                        }
                        timeXML.cancel();
                }
                break;

            case "response":
                System.out.println("RESPONSE message ready to be processed.");
                if (busMessage.getInfoIdentifier().equals(TOPIC_GETTASK) && busMessage.getDataFormat().equals("application/xml")) {
                    //GET ALL ORDERS FROM ERP
                    lastTask = busMessage.getContent();
                    System.out.println(lastTask);
                    //this.Consola.setText(Last_tarefa);
                    try {
                        handleRequest(lastTask);
                        System.out.println("Succesfully saved tarefa.xml");
                    } catch (IOException e) {
                        System.out.println("Error while saving tarefa.xml");
                        System.out.println(e.getMessage());
                    }

                } else if (busMessage.getInfoIdentifier().equals(TOPIC_NEWTASK) /*&& busMessage.getDataFormat().equals("application/json")*/) {
                    String json_str = busMessage.getContent();

                    System.out.println(json_str);//Provisoriamente para teste
                    System.out.println("Tarefa bem recebida");
                    //Tratar Json para saber se tarefa ficou atribuída
                } else if (busMessage.getInfoIdentifier().equals(TOPIC_CONCLUDETASK) /*&& busMessage.getDataFormat().equals("application/json")*/) {
                    String json_str = busMessage.getContent();

                    System.out.println(json_str);//Provisoriamente para teste
                    System.out.println("Acknowledge de tarefa concluida recebido");
                }
                break;
            case "stream":
                System.out.println("STREAM message ready to be processed.");
                switch (busMessage.getInfoIdentifier()) {
                    case TOPIC_UPDATEXML:
                        xmlStr = busMessage.getContent();
                        JSONObject coderollsJSONObject = new JSONObject(xmlStr);
                        System.out.println(xmlStr);//Provisoriamente para teste
                        String id;
                        if (coderollsJSONObject.get("id") != null)
                            id = coderollsJSONObject.get("id").toString();
                        else
                            id = "model";
                        String npart = coderollsJSONObject.get("nPart").toString();
                        String totalparts = coderollsJSONObject.get("totalParts").toString();
                        String content = coderollsJSONObject.get("xmlPart").toString();
                        System.out.println("n de partes: " + totalparts + " parte: " + npart);
                        completeXML(id, Integer.parseInt(npart), Integer.parseInt(totalparts), content);
                        break;
                    default:
                        //TODO: do nothing, isn't important. You can print a message for debug purposes.
                        System.out.println("Saiu default");
                        break;
                }
                break;
        }
        updateData();

    }

    Hashtable<Integer, String> xmlParts;
    String lastID;

    public void completeXML(String id, int npart, int totalparts, String part) {
        if (xmlParts == null) {
            xmlParts = new Hashtable<>();
            lastID = id;
        }
        idXML = id;
        if ((!xmlParts.containsKey(npart)) && id.equals(lastID)) {
            xmlParts.put(npart, part);
            if (xmlParts.size() == totalparts) {
                String xmlmessage = "";
                Set<Integer> keys = new TreeSet<>(xmlParts.keySet());

                for (Integer i : keys) {
                    xmlmessage = xmlmessage + xmlParts.get(i);
                    System.out.println("Adicionou");
                }
                consola.setText("Recebeu XML\n");

                xmlReceived = true;

                timeXML = new Timer();
                timeXML.schedule(new CheckXML(), 0, TimeUnit.MINUTES.toMillis(1));
                xmlParts = null;
                lastID = "";
                try {
                    write_xml_to_file("warehouse_recebido.xml", xmlmessage);
                    avisaNovoXML();
                    System.out.println("Succesfully saved warehouse_recebido.xml");
                } catch (IOException e) {
                    System.out.println("Error while saving warehouse_recebido.xml");
                    System.out.println(e.getMessage());
                }
            } else {
                System.out.println("Waiting for the remaining " + (totalparts - xmlParts.size()) + " parts of the warehouse model.");
            }
        } else {
            String xmlAnswer = new JSONObject()
                    .put("id", id)
                    .put("ack", "ERROR").toString();
            System.out.println(xmlAnswer);
            cm.SendMessageAsync(
                    Util.GenerateId(),
                    "response",
                    "mod_updateXMLstatus",
                    MODELADOR_ID,
                    "application/json",
                    xmlAnswer,
                    "1");
            xmlParts = null;
        }
    }

    public class CheckXML extends TimerTask {
        public void run() {
            try {
                xmlReceived = false;
            } catch (Exception ex) {
                System.out.println("Error running thread " + ex.getMessage());
            }
        }
    }

    //Grilo: Método que é chamado para que se guarde um request do ERP
    public void handleRequest(String xmlString) throws IOException {
        try {
            requestsManager.addRequest(xmlString);
        } catch (WarehouseConfigurationException e) {
            e.printStackTrace();
            //TODO: É PRECISO MAIS ALGUMA COISA?
        }
        write_xml_to_file("tarefa.xml", xmlString);
        Map<Agent, String> tasksAssignment = solver.solve();
        if (tasksAssignment != null) {
            for (Agent agent : tasksAssignment.keySet()) {
                sendTask(agent.getId(), tasksAssignment.get(agent));
            }
        }
        updateData();
    }

    public class CheckERP extends TimerTask {
        public void run() {
            try {
                if(checkERPTasksAutomatically) {
                    askERPTask();
                }
            } catch (Exception ex) {
                System.out.println("error running thread " + ex.getMessage());
            }
        }
    }

    public void playPacman(String agentid, String content) throws IOException, SAXException {
        String posx;
        String posy;
        System.out.println(content);
        DocumentBuilder db = null;
        try {
            db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        InputSource is = new InputSource(new StringReader(content));

        assert db != null;
        Document doc = db.parse(is);
        doc.getDocumentElement().normalize();
        NodeList path_nodes = doc.getElementsByTagName("Node");
        NodeList pick_nodes = doc.getElementsByTagName("Tarefa");


        for (int j = 0; j < pick_nodes.getLength(); j++) {
            Element element = (Element) pick_nodes.item(j);

            if (element.getNodeType() == Node.ELEMENT_NODE) {
                String wmscode = element.getElementsByTagName("Origem").item(0).getTextContent();
                String productid = element.getElementsByTagName("LinhaOrdem").item(0).getTextContent();
                Point2D.Float rack = warehouse.getWms(wmscode);

                pacmanSurface.addProduct(productid, rack);
            }
        }
        repaint();
        for (int j = 0; j < path_nodes.getLength(); j++) {
            Element element = (Element) path_nodes.item(j);

            if (element.getNodeType() == Node.ELEMENT_NODE) {
                posx = element.getElementsByTagName("x").item(0).getTextContent();
                posy = element.getElementsByTagName("y").item(0).getTextContent();
                posx = posx.replace(',', '.');
                posy = posy.replace(',', '.');
                float xx = Float.parseFloat(posx);
                float yy = Float.parseFloat(posy);

                pacmanSurface.updateAgent(agentid, new Point2D.Float(xx, yy));
                repaint();
                for (int i = 0; i < element.getElementsByTagName("Tarefa").getLength(); i++) {
                    pacmanSurface.removeProduct(element.getElementsByTagName("LinhaOrdem").item(i).getTextContent());
                }

                repaint();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        pacmanSurface.removeAgent(agentid);

        repaint();
    }


    public static void main(String[] args) {

        final String dir = System.getProperty("user.dir");
        //SERVICE BUS
        OutputStream output;
        try {
            output = new FileOutputStream(dir + "\\logPathPlanner.txt");
            printOut = new PrintStream(output);
            System.setOut(printOut);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Bridge.setVerbose(true);
        Bridge.setDebug(true);
        try {
            Bridge.init();
            File proxyAssembyFile = new File(dir + "/ClassLib.j4n.dll");
            Bridge.LoadAndRegisterAssemblyFrom(proxyAssembyFile);
        } catch (Exception e) {
            try {
                File proxyAssembyFile = new File(dir + "/lib/ClassLib.j4n.dll");
                Bridge.LoadAndRegisterAssemblyFrom(proxyAssembyFile);
            } catch (Exception e2) {
                System.out.println("Error");
                e2.printStackTrace();
            }
        }
        new PathPlanner().setVisible(true);
    }

    public WarehouseState getRequestsState() {
        return warehouseState;
    }

    public boolean isCheckERPTasksAutomatically() {
        return checkERPTasksAutomatically;
    }
}