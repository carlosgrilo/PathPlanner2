package gui.utils;


import newWarehouse.Prefab;
import newWarehouse.Prefabtype;
import newWarehouse.Warehouse;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class BackgroundSurface extends JPanel {
    private HashMap<Integer, LinkedList<Shape>> shapes;
    private static final Integer PREFAB_RACK = 0;
    private static final Integer PREFAB_STRUCTURE = 1;
    private static final Integer PREFAB_FLOORAREA = 2;
    private final double SENSIBILITY = 0.5;//m
    private boolean grid;
    public float AMPLIFY;
    private Warehouse warehouse;

    public BackgroundSurface(Warehouse warehouse, boolean grid, int largura) {

        this.warehouse = warehouse;

        shapes = generateShapes();
        this.grid = grid;
        super.setSize(largura, Math.round(largura * warehouse.getDepth() / warehouse.getWidth()));
        //super.setSize(largura, Math.round(largura * warehouse.getDepth() / warehouse.getWidth()));

    }

    public BackgroundSurface() {
        this.shapes = new HashMap<>();
        this.AMPLIFY = 1;
        this.warehouse = null;
    }

    public void setPrefabs(Warehouse warehouse){
        this.warehouse = warehouse;
        shapes = generateShapes();
        repaint();
    }

    private void paintBackground(Graphics2D g2) {

        g2.setPaint(Color.LIGHT_GRAY);

        if (grid) {
            for (int i = 0; i < getSize().width; i += 10) {
                Shape line = new Line2D.Float(i, 0, i, getSize().height);
                g2.draw(line);
            }

            for (int i = 0; i < getSize().height; i += 10) {
                Shape line = new Line2D.Float(0, i, getSize().width, i);
                g2.draw(line);
            }
        }

    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (warehouse !=null) {
            AMPLIFY = Math.min(((float) getSize().width) / warehouse.getArea().x, ((float) getSize().height) / warehouse.getArea().y);

            AffineTransform tx = new AffineTransform();
            tx.scale(AMPLIFY,AMPLIFY);

            shapes = generateShapes();
            LinkedList<Shape> drawables = new LinkedList<>();

            Shape r = new Rectangle2D.Float(0f, 0f, warehouse.getArea().x, warehouse.getArea().y);

            r = tx.createTransformedShape(r);
            drawables.add(r);

            Graphics2D g2 = (Graphics2D) g;
            g2.translate(getWidth(),0);
            g2.scale(-1.0,1.0);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            paintBackground(g2);
            Color[] colors = {Color.CYAN, Color.LIGHT_GRAY, Color.magenta};

            g2.setStroke(new BasicStroke(2));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
            for (Map.Entry<Integer, LinkedList<Shape>> entry : shapes.entrySet()) {
                Integer color_index = entry.getKey();
                LinkedList<Shape> shapes = entry.getValue();
                for (Shape s : shapes) {
                    Shape sr = r.getBounds2D().createIntersection(s.getBounds2D());
                    //sr=tx.createTransformedShape(sr);
                    g2.setPaint(Color.BLACK);
                    g2.draw(sr);
                    g2.setPaint(colors[color_index]);
                    g2.fill(sr);
                }
            }

            for (Shape s : drawables) {
                g2.setPaint(Color.BLACK);
                g2.draw(s);
                //g2.fill(s);
            }

        }

    }


    private Line2D.Float makeLine(int x1, int y1, int x2, int y2) {
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
        return (int) ((measure) * AMPLIFY);
    }

    public float descale(int measure){
        return (float) measure / AMPLIFY;
    }


    public HashMap<Integer, LinkedList<Shape>> generateShapes() {
        HashMap<Integer, LinkedList<Shape>> shapes = new HashMap<>();
        LinkedList<Shape> racks = new LinkedList<>();
        LinkedList<Shape> structures = new LinkedList<>();
        LinkedList<Shape> floorareas = new LinkedList<>();
        AMPLIFY = Math.min(((float) getSize().width) / warehouse.getArea().x, ((float) getSize().height) / warehouse.getArea().y);
        AffineTransform tx = new AffineTransform();

        for (Prefab prefab : warehouse.getPrefabList()) {

            Rectangle2D rec = (Rectangle2D) prefab.area.clone();

            tx.setToScale(AMPLIFY,AMPLIFY);
            if (prefab.rotation != 0)
                tx.rotate(Math.toRadians(-prefab.rotation), prefab.area.x, prefab.area.y);

            Shape newShape = tx.createTransformedShape(rec);
            if (prefab.type == Prefabtype.RACK) {
                racks.add(newShape);
            }
            if (prefab.type == Prefabtype.STRUCTURE) {
                structures.add(newShape);
            }
            if (prefab.type==Prefabtype.FLOORAREA) {
                floorareas.add(newShape);
            }

        }
        shapes.put(PREFAB_RACK, racks);
        shapes.put(PREFAB_STRUCTURE, structures);
        shapes.put(PREFAB_FLOORAREA, floorareas);
        return shapes;
    }

    public double posx(Prefab prefab){
        //Inverte a origem das coordenadas se no modelo for o canto superior direito
        return warehouse.getArea().x - prefab.area.x;
        //return prefab.getPosition().getX()
    }

    public double posx(Point ponto){
        //Inverte a origem das coordenadas se no modelo for o canto superior direito
        return warehouse.getArea().x - ponto.getX();
        //return prefab.getPosition().getX()
    }

    public double posx(double pontox){
        //Inverte a origem das coordenadas se no modelo for o canto superior direito
        return (warehouse.getArea().x - pontox);
        //return prefab.getPosition().getX()
    }
}
