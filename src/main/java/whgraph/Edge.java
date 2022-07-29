package whgraph;

import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class Edge {
    private boolean horizontal_edge = false;
    private boolean vertical_edge = false;
    private boolean oblique_edge = false;
    private ARWGraphNode start;
    private ARWGraphNode end;
    private final double weight;
    private boolean product_line;
    private final Point2D.Float location;
    private final int num_directions = 1;
    private final List<ARWGraphNode> products;

    public void setId(int id) {
        this.id = id;
    }

    public Point2D.Float getLocation() {
        return location;
    }

    private int id;

    public int getId() {
        return this.id;
    }

    public void setStart(ARWGraphNode start) {
        this.start = start;
    }

    public void setEnd(ARWGraphNode end) {
        this.end = end;
    }

    public ARWGraphNode getStart() {
        return this.start;
    }

    public int getIdOfStartGraphNode() {
        return this.start.getGraphNodeId();
    }

    public ARWGraphNode getEnd() {
        return this.end;
    }

    public int getIdOfEndGraphNode() {
        return this.end.getGraphNodeId();
    }

    public double getWeight() {
        return this.weight;
    }

    public Edge(ARWGraphNode s, ARWGraphNode e, double w, int id) {

        this.products = new ArrayList<>();
        this.start = s;
        this.end = e;
        this.weight = w;
        float x1 = s.getLocation().x;
        float x2 = e.getLocation().x;
        float y1 = s.getLocation().y;
        float y2 = e.getLocation().y;
        if (x1 == x2) {
            this.setVertical_edge(true);
        } else if (y1 == y2) {
            this.setHorizontal_edge(true);
        }else{
            this.setOblique_edge(true);
        }
        this.location = new Point2D.Float(x1 == x2 ? x2 : (x1 + x2) / 2, y1 == y2 ? y2 : (y1 + y2) / 2);
        this.id = id;
        this.product_line = false;
    }

    public boolean isProduct_line() {
        return product_line;
    }

    public void setProduct_line(boolean product_line) {
        this.product_line = product_line;
    }

    public Edge(ARWGraphNode s, ARWGraphNode e, double w, int id, boolean product_line) {
        this.products = new ArrayList<>();
        this.start = s;
        this.end = e;
        this.weight = w;
        float x1 = s.getLocation().x;
        float x2 = e.getLocation().x;
        float y1 = s.getLocation().y;
        float y2 = e.getLocation().y;
        if (x1 == x2) {
            this.setVertical_edge(true);
        } else if (y1 == y2) {
            this.setHorizontal_edge(true);
        }else{
            this.setOblique_edge(true);
        }
        this.location = new Point2D.Float(x1 == x2 ? x2 : (x1 + x2) / 2, y1 == y2 ? y2 : (y1 + y2) / 2);
        this.id = id;
        this.product_line = product_line;
    }

    @Override
    public String toString() {
        return "Edge " + id + " W " + getWeight() + " Start " + getStart().getGraphNodeId() + " End " + getEnd().getGraphNodeId() + " " + printDecline();
    }

    private String printDecline() {
        if (this.isVertical_edge()){
            return "VERTICAL";
        }else if(this.isHorizontal_edge()) {
            return "HORIZONTAL";
        }else if(this.isOblique_edge()){
            return "OBLIQUE";
        }
        return "NULL";
    }

    public ARWGraphNode getOtherEnd(ARWGraphNode ARWGraphNode) {
        if (this.getEnd().getGraphNodeId() == ARWGraphNode.getGraphNodeId()) {
            return this.getStart();
        } else {
            return this.getEnd();
        }
    }

    public void addProduct(ARWGraphNode product_agent) {
        this.products.add(product_agent);
    }

    public List<ARWGraphNode> getProducts() {
        return products;
    }

    public boolean isHorizontal_edge() {
        return horizontal_edge;
    }

    public void setHorizontal_edge(boolean horizontal_edge) {
        this.horizontal_edge = horizontal_edge;
    }

    public boolean isVertical_edge() {
        return vertical_edge;
    }

    public void setVertical_edge(boolean vertical_edge) {
        this.vertical_edge = vertical_edge;
    }

    public boolean isOblique_edge() {
        return oblique_edge;
    }

    public void setOblique_edge(boolean oblique_edge) {
        this.oblique_edge = oblique_edge;
    }


    public boolean intercepts(double x, double y, double radius){
        Circle circle=new Circle(radius);
        Line line=new Line(start.getX(),start.getY(),end.getX(),end.getY());
        return line.intersects(circle.getBoundsInLocal());
    }
}