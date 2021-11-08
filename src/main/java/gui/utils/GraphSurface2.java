package gui.utils;

import newWarehouse.Warehouse;
import whgraph.ARWGraph;
import whgraph.ARWGraphNode;
import whgraph.Edge;

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;

public class GraphSurface2 extends JPanel {
    private ARWGraph arwgraph;
    private double SENSIBILITY;
    private final int NODE_SIZE;
    Point startDrag, endDrag;
    public float AMPLIFY;
    public Warehouse warehouse;
    boolean editable;


    public GraphSurface2(ARWGraph graph, Warehouse warehouse, int node_size, int width) {
        this.arwgraph = graph;
        this.SENSIBILITY = 0.01;
        this.NODE_SIZE = node_size;
        this.warehouse = warehouse;
        editable = false;
        super.setSize(width, Math.round(width * warehouse.getDepth() / warehouse.getWidth()));
        AMPLIFY=1;
        //AMPLIFY = Math.min(((float) getSize().width) / warehouse.getWidth(), ((float) getSize().height) / warehouse.getDepth());
       //setOpaque(false);
    }

    public GraphSurface2(){
     NODE_SIZE = 5;
    }

    public void setArwgraph(ARWGraph arwgraph) {
        this.arwgraph = arwgraph;
    }

    public void setPrefabManager(Warehouse warehouse) {
        this.warehouse = warehouse;
        this.arwgraph.clear();
    }

    @Override
    public void paintComponent(Graphics g)  {
        super.paintComponent(g);
        if (warehouse !=null) {
            AMPLIFY = Math.min(((float) getSize().width) / warehouse.getArea().x, ((float) getSize().height) / warehouse.getArea().y);

            AffineTransform tx = new AffineTransform();
            tx.scale(-1, 1);

            Graphics2D g2 = (Graphics2D) g;
            g2.translate(getWidth(), 0);
            g2.scale(-1.0, 1.0);

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            float dot[] = {2f, 4f};
            BasicStroke solido = new BasicStroke(4f);
            BasicStroke dotted = new BasicStroke(1.0f,
                    BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER,
                    10.0f, dot, 0.0f);

            //super.paint(g2);
            g2.setPaint(Color.GRAY);
            g2.setStroke(solido);
            if (arwgraph != null) {
                for (ARWGraphNode node : arwgraph.getGraphNodes()) {

                    g2.drawOval(scale(node.getLocation().getX()) + (NODE_SIZE / 2),
                            scale(node.getLocation().getY()) - (NODE_SIZE / 2), NODE_SIZE, NODE_SIZE);
                    g2.drawString(node.printName(),  scale(node.getLocation().getX()) - (NODE_SIZE),
                            scale(node.getLocation().getY()) - (NODE_SIZE));
                }
                for (Edge e : arwgraph.getEdges()) {
                    Shape r = makeLine(
                            scale(e.getStart().getLocation().getX()), scale(e.getStart().getLocation().getY()),
                            scale(e.getEnd().getLocation().getX()), scale(e.getEnd().getLocation().getY()));
                    if (!editable)
                        g2.setStroke(dotted);

                    g2.draw(r);
                }

            }
        }
    }


    public Line2D.Float makeLine(int x1, int y1, int x2, int y2) {
        if (isSensibleX(x1, x2)) {
            x2 = x1;
        }
        if (isSensibleY(y1, y2)) {
            y2 = y1;
        }
        return new Line2D.Float(x1, y1, x2, y2);
    }

    private boolean isSensibleX(int x1, int x2) {
        return Math.abs(x1 - x2) < scale(SENSIBILITY);
    }

    private boolean isSensibleY(int y1, int y2) {
        return Math.abs(y1 - y2) < scale(SENSIBILITY);
    }

    public int scale(double measure){
        return (int) (measure * AMPLIFY);
    }

    public float descale(int measure){
        return (float) measure / AMPLIFY;
    }

}
