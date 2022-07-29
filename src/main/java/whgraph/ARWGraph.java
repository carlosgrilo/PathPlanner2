package whgraph;


import newWarehouse.Warehouse;
import orderpicking.GNode;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import pathfinder.Graph;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ARWGraph {
    private List<ARWGraphNode> nodes;
    private List<Edge> edges;

    private static final double MIN_NODE_DISTANCE = 50e-2;

    public ARWGraph() {
        nodes = new ArrayList<>();
        edges = new ArrayList<>();
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public int getNumberOfEdges() {
        return edges.size();//numberOfEdges;
    }

    public void createGraphNode(ARWGraphNode node) {
        this.nodes.add(node);
    }

    public void createGraphNode(float x, float y, GraphNodeType type) {
        int index = -1;
        do {
            index++;
        } while (findNode(index) != null);
        this.nodes.add(new ARWGraphNode(index, x, y, type));
    }


    public void createEdge(Edge edge) {
        edge.getStart().addNeighbour(edge);
        edge.getEnd().addNeighbour(edge);
        this.edges.add(edge);
    }

    public int getNumberOfNodes() {
        return this.nodes.size();
    }

    public List<ARWGraphNode> getGraphNodes() {
        return nodes;
    }
/*
    public ARWGraphNode getTrueNode(ARWGraphNode ARWGraphNode) {
        for (int i = 0; i < getGraphNodes().size(); i++) {
            if (getGraphNodes().get(i).getGraphNodeId() == ARWGraphNode.getGraphNodeId()) {
                return getGraphNodes().get(i);
            }
        }
        return null;
    }
*/

    @Override
    public ARWGraph clone() {

        ARWGraph graph = new ARWGraph();

        for (ARWGraphNode node : getGraphNodes()) {
            graph.createGraphNode(node.clone());
        }
        for (Edge edge : this.edges) {
            graph.makeNeighbors(graph.findNode(edge.getStart().getGraphNodeId()),
                    graph.findNode(edge.getEnd().getGraphNodeId()), edge.isProduct_line());
        }
        return graph;
    }

    public void setgraphNodes(List<ARWGraphNode> nodes) {
        this.nodes = nodes;
    }

    public int getMaxIdNodes(){
        int maxid = 0;
        for (ARWGraphNode node: nodes) {
            int id = node.getGraphNodeId();
            if (id > maxid)
                maxid = id;
        }
        return maxid;
    }

    public void removeNode(ARWGraphNode node) {
        if (node != null) {
            nodes.remove(node);
            List<Edge> toRemove = new ArrayList<>();
            for (Edge edge : edges) {
                if (edge.getEnd().equals(node) || edge.getStart().equals(node)) {
                    toRemove.add(edge);
                }
            }
            edges.removeAll(toRemove);
        }
    }

    public ARWGraphNode findNode(int node_id) {
        for (int i = 0; i < this.getGraphNodes().size(); i++) {
            if (this.getGraphNodes().get(i).getGraphNodeId() == node_id) {
                return getGraphNodes().get(i);
            }
        }
        return null;
    }

    public List<ARWGraphNode> getProducts() {
        List<ARWGraphNode> products = new ArrayList<>();
        for (int i = 0; i < getGraphNodes().size(); i++) {
            if (getGraphNodes().get(i).getType() == GraphNodeType.PRODUCT) {
                products.add(getGraphNodes().get(i));
            }
        }
        return products;
    }

    public ARWGraphNode findClosestNode(float x, float y) {
        ARWGraphNode closest = null;
        for (int i = 0; i < getGraphNodes().size(); i++) {
            ARWGraphNode node = getGraphNodes().get(i);
            if (closest == null || node.getDistance(x, y) <= closest.getDistance(x, y)) {
                closest = node;
            }
        }
        return closest;
    }

    public ARWGraphNode findClosestNode(float x, float y, float sensibility) {
        for (int i = 0; i < getGraphNodes().size(); i++) {
            ARWGraphNode node = getGraphNodes().get(i);
            if (x - sensibility < node.getLocation().getX() && node.getLocation().getX() < x + sensibility) {
                if (y - sensibility < node.getLocation().getY() && node.getLocation().getY() < y + sensibility) {
                    return node;
                }
            }
        }
        return null;
    }

    public int getDimensionX() {
        ARWGraphNode mostRight = null;
        for (int i = 0; i < getGraphNodes().size(); i++) {
            if (mostRight == null || mostRight.getLocation().getX() < getGraphNodes().get(i).getLocation().getX()) {
                mostRight = getGraphNodes().get(i);
            }
        }
        return Math.round(mostRight.getLocation().x);
    }

    public int getDimensionY() {
        ARWGraphNode mostBottom = null;
        for (int i = 0; i < getGraphNodes().size(); i++) {
            if (mostBottom == null || mostBottom.getLocation().getY() < getGraphNodes().get(i).getLocation().getY()) {
                mostBottom = getGraphNodes().get(i);
            }
        }
        return Math.round(mostBottom.getLocation().y);
    }

    public ARWGraphNode getLastNode() {
        if (nodes.size() > 0) {
            return nodes.get(nodes.size() - 1);
        }
        return null;
    }

    public void makeNeighbors(ARWGraphNode start_node, ARWGraphNode end_node, boolean product_line) {
        try {
            Edge e = findEdge(start_node, end_node);
            if (e == null) {
                e = new Edge(start_node, end_node, start_node.getDistance(end_node), edges.size(), product_line);
                e.setEnd(start_node);
                e.setStart(end_node);
                createEdge(e);
            } else {
                e.setProduct_line(product_line);
            }
        } catch (Exception e) {
            System.out.println("Cant make neighbors");
        }
    }

    private Edge findEdge(ARWGraphNode start_node, ARWGraphNode end_node) {
        for (Edge edge : edges) {
            if (edge.getEnd() == end_node && edge.getStart() == start_node) {
                return edge;
            }
            if (edge.getEnd() == start_node && edge.getStart() == end_node) {
                return edge;
            }
        }
        return null;
    }

    public void clear() {
        nodes.clear();
        edges.clear();
    }
    public double distancetoNeighborEdge(float x, float y){
        double distance = 1e6;
        double nx, ny, m;
        double size;

        for (Edge edge : edges) {
            double x1 = edge.getStart().getLocation().getX();
            double y1 = edge.getStart().getLocation().getY();
            double x2 = edge.getEnd().getLocation().getX();
            double y2 = edge.getEnd().getLocation().getY();
            if (Math.abs(x2 - x1) > 1e-3) {
                m = (y2 - y1) / (x2 - x1);
                nx = (2 * x - m * (y1 - y)) / (m * m + 2);
                ny = y1 + m * nx;
            }
            else {
                nx = x2;
                ny = y;
            }
            nx = Math.max(Math.min(nx, Math.max(x1, x2)), Math.min(x1, x2));
            ny = Math.max(Math.min(ny, Math.max(y1, y2)), Math.min(y1, y2));
            size = Math.sqrt(Math.pow(nx - x, 2) + Math.pow(ny - y, 2));
            if (size < distance){
                distance = size;
            }
        }
        return distance;
    }

    public ARWGraphNode insertNode(ARWGraphNode node){

        double distance = 1e6;
        double nx = 0, ny = 0, bestx = 0, besty = 0, m;
        double size;
        Edge closestedge = null;
        for (Edge edge : edges) {
            double x1 = edge.getStart().getLocation().getX();
            double y1 = edge.getStart().getLocation().getY();
            double x2 = edge.getEnd().getLocation().getX();
            double y2 = edge.getEnd().getLocation().getY();
            if (Math.abs(x2 - x1) > 1e-3) {
                m = (y2 - y1) / (x2 - x1);
                nx = (2 * node.getX() - m * (y1 - node.getY())) / (m * m + 2);
                ny = y1 + m * nx;
            } else {
                nx = x2;
                ny = node.getY();
            }

            nx = Math.max(Math.min(nx, Math.max(x1, x2)),Math.min(x1, x2));
            ny = Math.max(Math.min(ny, Math.max(y1, y2)),Math.min(y1, y2));
            size = Math.sqrt(Math.pow(nx - node.getX(), 2) + Math.pow(ny - node.getY(), 2));

            if (size < distance){
                distance = size;
                closestedge = edge;
                bestx = nx;
                besty = ny;
            }
        }
        if (closestedge != null) {
            node.setLocation(new Point2D.Float((float) bestx, (float) besty));

            ARWGraphNode existente = findClosestNode(node.getX(), node.getY());

            if ((Math.abs(existente.getX() - node.getX()) > MIN_NODE_DISTANCE) || (Math.abs(existente.getY() - node.getY()) > MIN_NODE_DISTANCE)) {
                Edge novo1 = new Edge(closestedge.getStart(), node, 0, 1);
                createGraphNode(node);
                createEdge(novo1);
                novo1 = new Edge(node,closestedge.getEnd(), 0, 1);
                createEdge(novo1);
                edges.remove(closestedge);
            } else
                return existente;
        }
        return node;
    }

    public void removeIntersection(float posx, float posy, float corridorwidth){
        List<Edge> edgestoremove =new ArrayList<>(edges);
        ARWGraphNode node=findClosestNode(posx,posy,corridorwidth);
        if (node!=null)
            removeNode(node);
        for(Edge edge : edgestoremove){
            if (edge.intercepts(posx, posy, corridorwidth))
                edges.remove(edge);
        }
    }
    public Graph getPathGraph(){

        Set<GNode> nos = new HashSet<>();
        Map<String, Set<String>> arestas = new HashMap<>();

        for (ARWGraphNode arwnode: getGraphNodes())
        {
            nos.add(new GNode(Integer.toString(arwnode.getGraphNodeId()), Integer.toString(arwnode.getGraphNodeId()),
                    arwnode.getX(), arwnode.getY()));
        }

        Set <String> conj;
        for  (Edge edge: edges){
            if ((conj=arestas.get(Integer.toString(edge.getIdOfStartGraphNode())))!=null)
                conj.add(Integer.toString(edge.getIdOfEndGraphNode()));

            else
                arestas.put(Integer.toString(edge.getIdOfStartGraphNode()),
                    Stream.of(Integer.toString(edge.getIdOfEndGraphNode())).collect(Collectors.toSet()));

            if ((conj=arestas.get(Integer.toString(edge.getIdOfEndGraphNode())))!=null)
                conj.add(Integer.toString(edge.getIdOfStartGraphNode()));
            else
                arestas.put(Integer.toString(edge.getIdOfEndGraphNode()),
                    Stream.of(Integer.toString(edge.getIdOfStartGraphNode())).collect(Collectors.toSet()));
        }

        return new Graph<>(nos, arestas);
    }


    public void readGraphFile(File file) {
        clear();
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();
            Element graphElement = doc.getDocumentElement();
            for (int i = 0; i < graphElement.getChildNodes().getLength(); i++) {
                if (graphElement.getChildNodes().item(i).getNodeName().equals("Nodes")) {
                    nodes = parseNodes(graphElement.getChildNodes().item(i));
                }
            }
            setgraphNodes(nodes);
            for (int i = 0; i < graphElement.getChildNodes().getLength(); i++) {
                if (graphElement.getChildNodes().item(i).getNodeName().equals("Edges")) {
                    edges = parseEdges(graphElement.getChildNodes().item(i));
                }
            }

        } catch (SAXException | IOException | ParserConfigurationException e) {
            e.printStackTrace();
        }
    }


    private List<ARWGraphNode> parseNodes(Node item) {
        List<ARWGraphNode> nodes = new ArrayList<>();
        for (int i = 0; i < item.getChildNodes().getLength(); i++) {
            if (item.getChildNodes().item(i).getNodeName().equals("Node")) {
                String[] loc = item.getChildNodes().item(i).getAttributes().getNamedItem("loc").getNodeValue().split(",");
                //Para compatibilidade com versoes anteriores, mantem-se a coordenada z.
                ARWGraphNode node = new ARWGraphNode(Integer.parseInt(item.getChildNodes().item(i).getAttributes().getNamedItem("id").getNodeValue()),
                        Float.parseFloat(loc[0]), Float.parseFloat(loc[1]),
                        Float.parseFloat(loc[2]),
                        GraphNodeType.valueOf(item.getChildNodes().item(i).getAttributes().getNamedItem("type").getNodeValue()));
                if (Boolean.parseBoolean(item.getChildNodes().item(i).getAttributes().getNamedItem("contains_product").getNodeValue())) {
                    node.setContains_product(true);
                }
                nodes.add(node);
            }
        }
        return nodes;
    }

    private List<Edge> parseEdges(Node item) {
        for (int i = 0; i < item.getChildNodes().getLength(); i++) {
            if (item.getChildNodes().item(i).getNodeName().equals("Edge")) {
                ARWGraphNode end = findNode(Integer.parseInt(item.getChildNodes().item(i).getAttributes().item(0).getNodeValue()));
                Boolean product_line = Boolean.parseBoolean(item.getChildNodes().item(i).getAttributes().item(1).getNodeValue());
                ARWGraphNode start = findNode(Integer.parseInt(item.getChildNodes().item(i).getAttributes().item(2).getNodeValue()));
                makeNeighbors(start, end, product_line);
            }
        }
        return getEdges();
    }


    public String generateXMLGraphString() {
        String xmlString = "";
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("Graph");

            doc.appendChild(rootElement);
            //Node List
            Element nodes = doc.createElement("Nodes");
            for (ARWGraphNode node : getGraphNodes()) {
                Element nodeElement = doc.createElement("Node");
                // set attribute to node element
                Attr attr_id = doc.createAttribute("id");
                attr_id.setValue(node.getGraphNodeId() + "");
                nodeElement.setAttributeNode(attr_id);

                Attr attr_type = doc.createAttribute("type");
                attr_type.setValue(node.getType() + "");
                nodeElement.setAttributeNode(attr_type);
                Attr contains_product = doc.createAttribute("contains_product");
                contains_product.setValue(node.contains_product() + "");
                nodeElement.setAttributeNode(contains_product);
                Attr attr_loc = doc.createAttribute("loc");
                String loc=node.getLocation().x+","+node.getLocation().y+",0";
                //Por uma questão de compatibilidade com a versao anterior, mantem-se a posicao Z
                attr_loc.setValue(loc);
                nodeElement.setAttributeNode(attr_loc);
                nodes.appendChild(nodeElement);
            }
            rootElement.appendChild(nodes);

            Element edges = doc.createElement("Edges");
            for (Edge edge : getEdges()) {
                Element edgeElement = doc.createElement("Edge");

                Attr attr_start = doc.createAttribute("start");
                attr_start.setValue(edge.getStart().getGraphNodeId() + "");
                edgeElement.setAttributeNode(attr_start);

                Attr attr_end = doc.createAttribute("end");
                attr_end.setValue(edge.getEnd().getGraphNodeId() + "");
                edgeElement.setAttributeNode(attr_end);

                Attr attr_product_line = doc.createAttribute("product_line");
                attr_product_line.setValue(edge.isProduct_line() + "");
                edgeElement.setAttributeNode(attr_product_line);

                edges.appendChild(edgeElement);
            }
            rootElement.appendChild(edges);

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer;

            transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            StringWriter writer = new StringWriter();

            //transform document to string
            transformer.transform(new DOMSource(doc), new StreamResult(writer));

            xmlString = writer.getBuffer().toString();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return xmlString;

    }

    public void createGraph(Warehouse warehouse, float corridorwidth){
        ArrayList<Line2D.Float> linhas = warehouse.createClearPaths(corridorwidth);

        clear();
        ARWGraphNode start = new ARWGraphNode(getNumberOfNodes(),linhas.get(0).x1, linhas.get(0).y1,  GraphNodeType.SIMPLE);
        createGraphNode(start);
        ARWGraphNode end = new ARWGraphNode(getNumberOfNodes(),linhas.get(0).x2, linhas.get(0).y2,  GraphNodeType.SIMPLE);
        createGraphNode(end);
        createEdge(new Edge(start,end,1,getNumberOfEdges() ));

        for (int i=1; i< linhas.size();i++){
            Line2D.Float linhatual=linhas.get(i);
            start = findClosestNode(linhatual.x1, linhatual.y1, 0.05f);
            if (start==null) {
                start = new ARWGraphNode(getNumberOfNodes(), linhatual.x1, linhatual.y1, GraphNodeType.SIMPLE);
                createGraphNode(start);
            }
            end = findClosestNode(linhatual.x2, linhatual.y2, 0.05f);
            if (end==null) {
                end = new ARWGraphNode(getNumberOfNodes(), linhatual.x2, linhatual.y2, GraphNodeType.SIMPLE);
                createGraphNode(end);
            }
            createEdge(new Edge(start,end,1,getNumberOfEdges()));

        }


    }

}
