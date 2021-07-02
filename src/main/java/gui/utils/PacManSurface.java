package gui.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Hashtable;

public class PacManSurface extends JComponent  {

    Point2D.Float startNode;
    Hashtable<String,Point2D.Float> operators;
    Hashtable<String,Point2D.Float> products;
    BackgroundSurface background;
    int nodeSize;

    public PacManSurface(BackgroundSurface background, int nodeSize) {
        this.startNode = new Point2D.Float((float) 1.0, (float) 0.0);
        this.background = background;
        this.nodeSize = nodeSize;
        operators = new Hashtable<>();
        products = new Hashtable<>();
    }

    public void addAgent(String agentid, Point2D.Float node){
        operators.put(agentid, node);
    }

    public void removeAgent(String agentid){
        operators.remove(agentid);
    }

    public void updateAgent(String agentid, Point2D.Float node){
        addAgent(agentid, node);
    }

    public void addProduct(String productid, Point2D.Float node){
        products.put(productid, node);
    }

    public void removeProduct(String productid){
        products.remove(productid);
    }

    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        int width = this.getWidth();

        //super.paint(g2, c);
        g2.setPaint(Color.BLUE);

        //Displaying Key and value pairs
        for(String agentid: operators.keySet()){

            Point2D.Float pos = operators.get(agentid);
            g2.drawString(
                    agentid.substring(0, 3),
                    width-background.scale(pos.getX()),
                    background.scale(pos.getY()));
            g2.fillOval(
                    width-background.scale(pos.getX()) - nodeSize / 2,
                    background.scale(pos.getY()) - nodeSize / 2, nodeSize, nodeSize);
        }

        for (String productid: products.keySet()){
            Point2D.Float pos = products.get(productid);
            g2.setPaint(Color.BLACK);
            g2.drawOval(
                    width-background.scale(pos.getX())-3*nodeSize/8,
                    background.scale(pos.getY())-3*nodeSize/8, 3 * nodeSize / 4, 3 * nodeSize / 4);

            g2.setPaint(Color.GREEN);
            g2.fillOval(
                    width-background.scale(pos.getX())-3*nodeSize/8,
                    background.scale(pos.getY())-3*nodeSize/8, 3 * nodeSize / 4, 3 * nodeSize / 4);
        }
    }

}