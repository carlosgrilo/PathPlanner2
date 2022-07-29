
package arwstate;

import orderpicking.GNode;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import pathfinder.Graph;
import pathfinder.GraphNode;
import pathfinder.Route;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

public class Task {
    private Request request;
    private List<Pick> picks;

    private double massCenterX;
    private double massCenterY;

    private Graph<GNode> graph;
    private Agent agent;
    private Route route;

    public Task(Request request) {
        this.request = request;
        picks = new ArrayList<>();
    }

    public Task copy(){
        Task task = new Task(request);
        task.setAgent(agent);
        task.getPicks().addAll(picks);
        task.setGraph(graph);
        return task;
    }

    public void computeMassCenter(){
        massCenterX = massCenterY = 0;
        for(Pick pick : picks){
            massCenterX += pick.getNode().getX();
            massCenterY += pick.getNode().getY();
        }
        massCenterX /= picks.size();
        massCenterY /= picks.size();
    }

    public Request getRequest() {
        return request;
    }

    public List<Pick> getPicks() {
        return picks;
    }

    public void setPicks(List<Pick> picks) {
        this.picks = picks;
    }

    public void removePicks(List<Pick> pickstoremove){
        picks.removeAll(pickstoremove);
    }

    public void addPick(Pick pick){
        picks.add(pick);
    }

    public double getMassCenterX() {
        return massCenterX;
    }

    public double getMassCenterY() {
        return massCenterY;
    }

    public Graph<GNode> getGraph() {
        return graph;
    }

    public void setGraph(Graph<GNode> graph) {
        this.graph = graph;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("<Task>\n");
        for (Pick pick : picks) {
            sb.append(pick);
        }
        sb.append("</Task>\n");
        return sb.toString();
    }


    //////////////// XML PROCESSING ////////////////

    public String XMLPath() {
        try {
            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();

            // root element
            Element root = document.createElement("Task");
            root.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
            root.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            Attr attrAgent = document.createAttribute("agent-id");
            attrAgent.setValue(agent.getId());
            root.setAttributeNode(attrAgent);
            Attr attRoot = document.createAttribute("id");
            String taskId = getGraph().getPicksAtNode().keySet().toString();
            attRoot.setValue(taskId);
            root.setAttributeNode(attRoot);
            document.appendChild(root);

            Element pathElement = document.createElement("Path");

            List<GraphNode> nodes = (List<GraphNode>) route.getNodes();
            for (GraphNode node: nodes){
                String nodeId = node.getId();
                //Sugestão para name: "node-id"
                //Sugestão para nattr: nodeAttr
                Attr nattr = document.createAttribute("id");
                nattr.setValue(nodeId);
                Element nodeXML = document.createElement("Node");
                nodeXML.setAttributeNode(nattr);

                Element position = document.createElement("Position");

                Element pos = document.createElement("x");
                pos.appendChild(document.createTextNode(String.format(Locale.US, "%.2f",graph.getNode(nodeId).getX())));
                position.appendChild(pos);

                pos = document.createElement("y");
                pos.appendChild(document.createTextNode(String.format(Locale.US,"%.2f",graph.getNode(nodeId).getY())));
                position.appendChild(pos);

                pos = document.createElement("z");
                pos.appendChild(document.createTextNode("0.0"));
                position.appendChild(pos);

                nodeXML.appendChild(position);

                Map<Integer, ArrayList<Pick>> picksAtNode = graph.getPicksAtNode();

                if (picksAtNode.containsKey(Integer.parseInt(nodeId))) {
                    ArrayList<Pick> picks1 = picksAtNode.get(Integer.parseInt(nodeId));

                    for (Pick pick : picks1) {
                        //Elementos todos em inglês
                        Element product = document.createElement("Tarefa");
                        Element lineProd = document.createElement("Ordem");
                        lineProd.appendChild(document.createTextNode(pick.getOrderID()));
                        product.appendChild(lineProd);
                        lineProd = document.createElement("LinhaOrdem");
                        lineProd.appendChild(document.createTextNode(pick.getOrderLine()));
                        product.appendChild(lineProd);
                        lineProd = document.createElement("Produto");
                        lineProd.appendChild(document.createTextNode(pick.getId()));
                        product.appendChild(lineProd);
                        lineProd = document.createElement("Quantidade");
                        lineProd.appendChild(document.createTextNode(pick.getQuantity()));
                        product.appendChild(lineProd);
                        lineProd = document.createElement("Origem");
                        lineProd.appendChild(document.createTextNode(pick.getOrigin()));
                        product.appendChild(lineProd);
                        lineProd = document.createElement("Destino");
                        lineProd.appendChild(document.createTextNode(pick.getDestiny()));
                        product.appendChild(lineProd);
                        nodeXML.appendChild(product);
                    }

                }
                pathElement.appendChild(nodeXML);
            }
            root.appendChild(pathElement);

            // create the xml string
            //transform the DOM Object to an XML string
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformerFactory.setAttribute("indent-number", 2);
            DOMSource domSource = new DOMSource(document);
            StringWriter writer = new StringWriter();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(document), new StreamResult(writer));

            String xmlPath = writer.getBuffer().toString();
            return xmlPath;
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        }

        return "";
    }

    public static List<Pick> parseXMLConcludedTask(String content) {
        try {
            System.out.println(content);
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(content));

            Document doc = db.parse(is);
            doc.getDocumentElement().normalize();
            NodeList taskNodes = doc.getElementsByTagName("Tarefa");

            List<Pick> picks = new LinkedList<>();

            for (int i = 0; i < taskNodes.getLength(); i++) {

                Element element = (Element) taskNodes.item(i);

                if (element.getNodeType() == Node.ELEMENT_NODE) {
                    Pick pick = new Pick(
                            element.getElementsByTagName("Ordem").item(0).getTextContent(),
                            element.getElementsByTagName("LinhaOrdem").item(0).getTextContent(),
                            element.getElementsByTagName("Produto").item(0).getTextContent(),
                            element.getElementsByTagName("Quantidade").item(0).getTextContent(),
                            element.getElementsByTagName("Origem").item(0).getTextContent(),
                            element.getElementsByTagName("Destino").item(0).getTextContent());
                    picks.add(pick);
                }
            }
            return picks;
        } catch(ParserConfigurationException e){
            //return null;
            e.printStackTrace();
        } catch(IOException e){
            e.printStackTrace();
        } catch(SAXException e){
            e.printStackTrace();
        }
        return null;
    }

}
