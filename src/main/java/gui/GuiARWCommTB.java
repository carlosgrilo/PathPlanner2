package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;

import xmlutils.XMLfuncs;
import classlib.BusMessage;
import classlib.CommunicationManager;
import classlib.TopicsConfiguration;
import classlib.Util;
import communication.esb_callbacks;
import net.sf.jni4net.Bridge;
import org.json.JSONObject;

public class GuiARWCommTB extends JFrame {
    private JButton newoperator;
    private JButton oper_available;
    private JButton oper_concluded;
    private JButton RecebeDisponib;
    private JButton EnviaTarefa;
    private JButton RecebeConclusao;
    private JButton EnviaConclusao;
    private JTextArea Consola;

    public static final String PLANNER_NAME = "planeador";
    public static final String CLIENT_ID_PREFIX = "ra";
    public static final String ERP_ID = "ERP";
    public static final String RA_ID = "ra";
    public static final String LOC_APROX_ID = "locaproximada";
    public static final String MODELADOR_ID = "modelador";
    public static final int NUM_OPERATORS = 1;
    public static final String OP_ID = "1";
    public static final String WAREHOUSE_FILE = "warehouse_model_lab.xml";
    public static final String TOPIC_UPDATEXML = "mod_updateXML";
    public static final String TOPIC_ACKXML = "mod_updateXMLstatus";
    public static final String TOPIC_OPAVAIL = "available";
    public static final String TOPIC_NEWOP = "newOperator";
    public static final String TOPIC_GETTASK = "getTarefa";
    public static final String TOPIC_ENDTASK = "endTask";
    public static final String TOPIC_NEWTASK = "newTask";
    public static final String TOPIC_CONCLUDETASK = "setTarefaFinalizada";

    CommunicationManager cm;
    esb_callbacks Checkbus;
    String Last_tarefa;

    int agentNum;

    private String taskReceivedXML;

    public GuiARWCommTB() {
        super("ARWARE Gui Comm TestBed");

        initComponents();

        setSize(900, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        agentNum = 0;
    }

    private void initComponents() {
        newoperator = new JButton("Novo Operador");
        oper_available = new JButton("Operador disponível");
        oper_concluded = new JButton("Operador terminou");
        RecebeDisponib = new JButton("Recebe Disp.");
        EnviaTarefa = new JButton("Envia Tarefa");
        RecebeConclusao = new JButton("Recebe Concl.");
        EnviaConclusao = new JButton("Envia Conclusao");
        //ExitButton = new JButton("FIM") ;

        Consola = new JTextArea(20, 80);
        Consola.setAutoscrolls(true);

        setLayout(new FlowLayout());

        add(newoperator);
        add(oper_available);
        add(oper_concluded);
        add(EnviaTarefa);
        add(RecebeConclusao);
        add(EnviaConclusao);
        //add(ExitButton);
        add(Consola);

        newoperator.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                // delegate to event handler method
                String message = new JSONObject()
                        .put("request", "xml").toString();
                cm.SendMessageAsync(Util.GenerateId(), "request", "newOperator", PLANNER_NAME, "application/json", message, "1");
            }
        });

        EnviaTarefa.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                // delegate to event handler method
                Executa_EnviaXML();
            }
        });

        oper_available.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                // delegate to event handler method
                String message = new JSONObject()
                        .put("available", "yes")
                        .put("posicaox", 20.000)
                        .put("posicaoy", 58.000).toString();

                cm.SendMessageAsync(Util.GenerateId(), "request", "available", PLANNER_NAME, "application/json", message, "1");

            }
        });

        oper_concluded.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                cm.SendMessageAsync(Util.GenerateId(), "request", "endTask", PLANNER_NAME, "application/xml", taskReceivedXML, "1");
            }
        });

        EnviaConclusao.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                // delegate to event handler method
                Executa_ConcluiTarefa();
            }
        });

        Checkbus = new esb_callbacks();

        String clientID = CLIENT_ID_PREFIX + (Math.abs((new Random()).nextInt()) - 1);

        cm = new CommunicationManager(clientID, new TopicsConfiguration(), Checkbus);
        Checkbus.SetCommunicationManager(cm); // Está imbricado. Tentar ver se é possível alterar!
        Checkbus.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                Trata_Mensagens((BusMessage) evt.getNewValue());
            }
        });

        //cm.SubscribeContentAsync("mod_updateXML",MODELADOR_ID);

        System.out.println("CLIENT_ID: " + clientID);
        System.out.println("ERP_ID: " + ERP_ID);
        System.out.println("RA_ID: " + RA_ID + "[MAC]");
        System.out.println("LOC_APROX_ID: " + LOC_APROX_ID);
        System.out.println("MODELADOR_ID: " + MODELADOR_ID);

    }


    public void Executa_EnviaXML() {
        String xmlstring = "";
        try {
            xmlstring = read_xml_from_file("warehouse_model_lab.xml");
        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayList<String> lista = XMLfuncs.createBrokenXML(xmlstring, 30000);
        for (int i = 0; i < lista.size(); i++) {
            String jsonstr = lista.get(i);
            cm.SendStreamMessageAsync("mod_updateXML2", "application/json", jsonstr, "1");
        }
    }

    public void Executa_RecebeTarefa() {
        this.Consola.append("Pedida tarefa" + '\n');
        this.cm.SendMessageAsync(Util.GenerateId(), "request", "getTarefa", "ERP", "PlainText", "Dá-me uma tarefa!", "1");
    }


    public void Executa_EnviaTarefa() {
        try {
            String xmlString = read_xml_from_file("tasksim.xml");
            this.Consola.append(xmlString + '\n');
            cm.SendMessageAsync(Util.GenerateId(), "request", "newTask", "ra1", "application/xml", xmlString, "1");
        } catch (IOException e) {
            System.out.println("Error while reading tasksim.xml");
            System.out.println(e.getMessage());

        }

    }

    public void Executa_ConcluiTarefa() {
        try {
            String xmlString = read_xml_from_file("tarefa.xml");
            this.Consola.append(xmlString + '\n');
            cm.SendMessageAsync(Util.GenerateId(), "response", "taskconcluded", ERP_ID, "application/xml", xmlString, "1");
        } catch (IOException e) {
            System.out.println("Error while reading tasksim.xml");
            System.out.println(e.getMessage());

        }

    }

    public void Trata_Mensagens(BusMessage busMessage) {

        switch (busMessage.getMessageType()) {
            case "request":
                System.out.println("REQUEST message ready to be processed.");
                String identificador = busMessage.getInfoIdentifier();
                switch (identificador) {

                    case "available":
                    case "Disponivel":
                    case "newOperator":

                        String xml_str = busMessage.getContent();
                        System.out.println(xml_str);//Provisoriamente para teste
                        Consola.setText(xml_str);
                        String[] split = busMessage.getFromTopic().split("Topic");

                        if (identificador.equals("newOperator")) {
                            try {
                                String xml_armazem = read_xml_from_file(WAREHOUSE_FILE);
                                cm.SendMessageAsync((Integer.parseInt(busMessage.getId()) + 1) + "", "response",
                                        busMessage.getInfoIdentifier(), split[0], "application/xml", xml_armazem, "1");
                                System.out.println("Enviou XML");
                            } catch (IOException e) {
                                cm.SendMessageAsync((Integer.parseInt(busMessage.getId()) + 1) + "", "response",
                                        busMessage.getInfoIdentifier(), split[0], "application/json", "{'response':'ACK'}", "1");
                                System.out.println("Houve erro. Só enviou Ack");
                            }
                        } else {
                            System.out.println("É available");

                            cm.SendMessageAsync((Integer.parseInt(busMessage.getId()) + 1) + "", "response",
                                    busMessage.getInfoIdentifier(), split[0], "application/json", "{'response':'ACK'}", "1");
                            System.out.println("Enviou Ack!");
                        }
                        break;
                    case "endTask":
                        xml_str = busMessage.getContent();
                        System.out.println(xml_str);//Provisoriamente para teste
                        Consola.setText(xml_str);
                        split = busMessage.getFromTopic().split("Topic");
                        System.out.println("Recebeu tarefa concluída como request");//Provisoriamente para teste
                        Consola.setText(xml_str);
                        try {
                            write_modelador_xml_to_file("tarefa_concluida.xml", xml_str);

                            System.out.println("Succesfully saved concluded task>");
                        } catch (IOException e) {
                            System.out.println("Error while saving concluded task");
                            System.out.println(e.getMessage());
                        }
                        cm.SendMessageAsync((Integer.parseInt(busMessage.getId()) + 1) + "", "response",
                                busMessage.getInfoIdentifier(), split[0], "application/json", "{'response':'ACK'}", "1");
                        System.out.println("Enviou Ack!");

                        break;
                    case TOPIC_NEWTASK:
                        xml_str = busMessage.getContent();
                        taskReceivedXML = xml_str;

                        System.out.println(xml_str);//Provisoriamente para teste
                        System.out.println("Tarefa bem recebida");
                        //Tratar Json para saber se tarefa ficou atribuída

                }
                break;
            case "response":
                System.out.println("RESPONSE message ready to be processed.");
                if (busMessage.getInfoIdentifier().equals("getTarefa") && busMessage.getDataFormat().equals("application/xml")) {
                    //GET ALL ORDERS FROM ERP
                    Last_tarefa = busMessage.getContent();
                    this.Consola.setText(Last_tarefa);
                    try {
                        write_modelador_xml_to_file("tarefa.xml", Last_tarefa);

                        System.out.println("Succesfully saved tarefa.xml");
                    } catch (IOException e) {
                        System.out.println("Error while saving tarefa.xml");
                        System.out.println(e.getMessage());

                    }
                    //GASingleton.getInstance().parseTarefaXML(busMessage.getContent());
                    //List<Tarefa> products = mapper.readValue(busMessage.getContent(), List.class);

                } else if (busMessage.getInfoIdentifier().equals("endTask") && busMessage.getDataFormat().equals("application/xml")) {
                    String xml_str = busMessage.getContent();

                    System.out.println("Recebeu tarefa concluída como response");
                    Consola.setText(xml_str);
                    try {
                        write_modelador_xml_to_file("tarefa_concluida.xml", xml_str);

                        System.out.println("Succesfully saved concluded task");
                    } catch (IOException e) {
                        System.out.println("Error while saving concluded task");
                        System.out.println(e.getMessage());
                    }
                } else if (busMessage.getInfoIdentifier().equals(TOPIC_NEWTASK) && busMessage.getDataFormat().equals("application/xml")) {
                    String xml_str = busMessage.getContent();

                    System.out.println(xml_str);

                }

                break;
            case "stream":
                System.out.println("STREAM message ready to be processed.");
                switch (busMessage.getInfoIdentifier()) {
                    case "mod_updateXML":
                        String xml_str = busMessage.getContent();
                        JSONObject coderollsJSONObject = new JSONObject(xml_str);
                        System.out.println(xml_str);//Provisoriamente para teste
                        String id = "";
                        if (coderollsJSONObject.get("id") != null)
                            id = coderollsJSONObject.get("id").toString();
                        else
                            id = "model";
                        String npart = coderollsJSONObject.get("nPart").toString();
                        String totalparts = coderollsJSONObject.get("totalParts").toString();
                        String content = coderollsJSONObject.get("xmlPart").toString();
                        System.out.println("n de partes: " + totalparts + " parte: " + npart);
                        completeXML(id, Integer.parseInt(npart), Integer.parseInt(totalparts), content);
/*
                        Consola.setText(xml_str);
                        try {
                            write_modelador_xml_to_file("warehouse_recebido.xml", xml_str);

                            System.out.println("Succesfully saved warehouse_model.xml");
                        } catch (IOException e) {
                            System.out.println("Error while saving warehouse_model.xml");
                            System.out.println(e.getMessage());

                        }                    */
                        break;
                    default:
                        //TODO: do nothing, isn't important. You can print a message for debug purposes.
                        System.out.println("Saiu default");
                        break;
                }
                break;
        }

    }

    Hashtable<Integer, String> xmlparts;
    String lastid;

    public void completeXML(String id, int npart, int totalparts, String part) {
        if (xmlparts == null) {
            xmlparts = new Hashtable<Integer, String>();
            lastid = id;
        }
        if ((!xmlparts.containsKey(npart)) && id.equals(lastid)) {
            xmlparts.put(npart, part);
            if (xmlparts.size() == totalparts) {
                String xmlmessage = "";
                for (Integer i = 0; i < totalparts; i++) {
                    xmlmessage = xmlmessage + xmlparts.get(i);
                    System.out.println("Adicionou");

                }
                Consola.setText(xmlmessage);
                String xmlanswer = new JSONObject()
                        .put("id", id)
                        .put("ack", "OK").toString();

                cm.SendMessageAsync(Util.GenerateId(), "response", "mod_updateXMLstatus", MODELADOR_ID, "application/json",
                        xmlanswer, "1");
                xmlparts = null;
                lastid = "";
                try {
                    write_modelador_xml_to_file("warehouse_recebido.xml", xmlmessage);

                    System.out.println("Succesfully saved warehouse_recebido.xml");

                } catch (IOException e) {
                    System.out.println("Error while saving warehouse_recebido.xml");
                    System.out.println(e.getMessage());

                }
            } else {
                System.out.println("Waiting for the remaining " + new Integer(totalparts - xmlparts.size()).toString() + " parts of the warehouse model.");
            }
        } else {

            String xmlanswer = new JSONObject()
                    .put("id", id)
                    .put("ack", "ERROR").toString();
            System.out.println(xmlanswer);
            cm.SendMessageAsync(Util.GenerateId(), "response", "mod_updateXMLstatus", MODELADOR_ID, "application/json",
                    xmlanswer, "1");
            xmlparts = null;

        }
    }

    public void write_modelador_xml_to_file(String fileName, String str) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(fileName);
        byte[] strToBytes = str.getBytes();
        outputStream.write(strToBytes);
        outputStream.close();
    }

    public String read_xml_from_file(String fileName) throws IOException {
        return new String(Files.readAllBytes(Paths.get(fileName)));
    }


    public static void main(String[] args) {

        final String dir = System.getProperty("user.dir");
        //SERVICE BUS
        OutputStream output = null;

        //System.setOut(printOut);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String userInput;
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

        new GuiARWCommTB().setVisible(true);

    }
}