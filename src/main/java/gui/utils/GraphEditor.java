package gui.utils;

import newWarehouse.Warehouse;

import whgraph.ARWGraph;
import whgraph.ARWGraphNode;
import whgraph.Edge;
import whgraph.GraphNodeType;

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.NoSuchElementException;


public class GraphEditor extends JDialog {

    private double SENSIBILITY;
    private static int NODE_SIZE = 5;
    public Node_Action node_action = Node_Action.DRAW;

    private final BackgroundSurface background;

    public ARWGraph arwgraph;


    JMenuBar menuBar;
    JMenu menu;
    JMenuItem menuItem;
    Warehouse warehouse;
    float corridorwidth;

    public GraphEditor(Warehouse warehouse, ARWGraph arwgraph, float corridorwidth) throws HeadlessException {

        this.arwgraph = arwgraph;
        int width=1000, depth=1000;
        if (warehouse.getWidth()>warehouse.getDepth())
            depth= Math.round(1000*warehouse.getDepth()/warehouse.getWidth());
        else
            width=Math.round( 1000*warehouse.getWidth()/ warehouse.getDepth());

        this.setTitle("Graph Editor");
        setSize(width, depth);
        this.SENSIBILITY = 0.25;
        this.corridorwidth = corridorwidth;
        this.warehouse = warehouse;
        this.setModal(true);
        background = new BackgroundSurface(warehouse, true, width);
        LayerUI<JPanel> graphsurface = new GraphSurfaceEd(arwgraph, warehouse, SENSIBILITY, NODE_SIZE);
        JLayer<JPanel> jlayer = new JLayer<>(background, graphsurface);

        setLayout(new BorderLayout());

        setupMenuBar(arwgraph);



        JPanel panelButtonsNorth = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel node_action_label = new JLabel("Node Action");
        panelButtonsNorth.add(node_action_label, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        JButton button_draw = new JButton("Draw");
        panelButtonsNorth.add(button_draw, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        JButton button_remove = new JButton("Remove");
        panelButtonsNorth.add(button_remove, gbc);

        gbc.gridwidth = 2;

        button_draw.addActionListener(e -> node_action = Node_Action.DRAW);
        button_remove.addActionListener(e -> node_action = Node_Action.REMOVE);

        add(panelButtonsNorth, BorderLayout.NORTH);

        add(jlayer, BorderLayout.CENTER);

        this.setVisible(true);

        this.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
            }


            @Override
            public void keyPressed(KeyEvent e) {
                if ((e.getKeyCode() == KeyEvent.VK_Z) && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
                    getArwgraph().removeNode(getArwgraph().getLastNode());
                    background.repaint();
                    jlayer.repaint();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }

        });

    }

    private void setupMenuBar(ARWGraph arwgraph) {
        menuBar = new JMenuBar();

        //Build the first menu.
        menu = new JMenu("Graph");
        menu.setMnemonic(KeyEvent.VK_A);
        menu.getAccessibleContext().setAccessibleDescription(
                "File options");
        menuBar.add(menu);

        //a group of JMenuItems
        menuItem = new JMenuItem("New",
                KeyEvent.VK_N);
        menuItem.getAccessibleContext().setAccessibleDescription(
                "New graph");
        menuItem.addActionListener(e -> newGraph());
        menu.add(menuItem);
        menuItem = new JMenuItem("Automatic",
                KeyEvent.VK_A);
        menuItem.getAccessibleContext().setAccessibleDescription(
                "Generate graph automatically");
        menuItem.addActionListener(e -> autoGraph());
        menu.add(menuItem);
        menuItem = new JMenuItem("Export",
                KeyEvent.VK_T);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_1, InputEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription(
                "Export graph");
        menu.add(menuItem);
        menuItem.addActionListener(e -> exportGraph(arwgraph));

        menuItem = new JMenuItem("Import",
                KeyEvent.VK_T);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_2, InputEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription(
                "Import graph");
        menuItem.addActionListener(e -> LoadGraph());
        menu.add(menuItem);
        this.setJMenuBar(menuBar);
    }

    public ARWGraph getArwgraph() {
        return arwgraph;
    }

    private void newGraph() {
        arwgraph.clear();
        repaint();
    }

    private void autoGraph() {
        arwgraph.createGraph(warehouse, corridorwidth);
        repaint();
    }

    private void LoadGraph() {

        JFileChooser fc = new JFileChooser(new File("."));
        fc.setSelectedFile(new File("graph.xml"));
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

    private void exportGraph(ARWGraph ARWGraph) {
        JFileChooser fc = new JFileChooser(new File("."));
        fc.setSelectedFile(new File("graph.xml"));
        int returnVal = fc.showSaveDialog(this);
        try {
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                FileWriter fw = new FileWriter(fc.getSelectedFile());
                fw.write(ARWGraph.generateXMLGraphString());
                fw.close();
            }
        } catch (NoSuchElementException | IOException e2) {
            JOptionPane.showMessageDialog(this, "File format not valid", "Error!", JOptionPane.ERROR_MESSAGE);
        }
    }

    class GraphSurfaceEd extends GraphSurface {

        ARWGraphNode start_node;
        ARWGraphNode end_node;

        public GraphSurfaceEd(ARWGraph ARWGraph, Warehouse warehouse, double sensibility, int node_size) {
            arwgraph = ARWGraph;
            SENSIBILITY = sensibility;
            NODE_SIZE = node_size;
            this.warehouse = warehouse;
            this.editable = true;
        }

        public void setSensibility(int sensibility){
            SENSIBILITY = sensibility;
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            JLayer jlayer = (JLayer) c;
            jlayer.setLayerEventMask(
                    AWTEvent.MOUSE_EVENT_MASK |
                            AWTEvent.MOUSE_MOTION_EVENT_MASK
            );
        }

        @Override
        public void uninstallUI(JComponent c) {
            JLayer jlayer = (JLayer) c;
            jlayer.setLayerEventMask(0);
            super.uninstallUI(c);
        }
        @Override
        protected void processMouseEvent(MouseEvent e, JLayer l) {
            double dwidth= descale(l.getWidth());
            if (e.getID() == MouseEvent.MOUSE_PRESSED) {
                startDrag = new Point(e.getX(), e.getY());
                endDrag = startDrag;
                if (node_action == Node_Action.REMOVE) {
                    ARWGraphNode node = arwgraph.findClosestNode((float)dwidth - descale(e.getX()),
                            descale(e.getY()), (float) SENSIBILITY);
                    if (node != null) {
                        arwgraph.removeNode(node);
                    }
                }
                l.repaint();

            }

            if (e.getID() == MouseEvent.MOUSE_RELEASED) {
                if (!(startDrag.x == e.getX() && startDrag.y == e.getY() || node_action == Node_Action.REMOVE)) {

                    if (arwgraph.distancetoNeighborEdge(((float)dwidth - descale(startDrag.x)),
                            descale(startDrag.y)) < SENSIBILITY) {

                        //Insere um nó, se for necessário, na aresta mais próxima, se dentro da sensibilidade
                        //Dado que não é garantido que o nó seja o último, o id serve paar identificar o nó
                        //de início.
                        start_node = arwgraph.insertNode(new ARWGraphNode(arwgraph.getMaxIdNodes() + 1,
                                ((float)dwidth - descale(startDrag.x)),
                                descale(startDrag.y), GraphNodeType.SIMPLE));
                        startDrag = new Point(scale((float)dwidth - start_node.getLocation().getX()),
                                scale(start_node.getLocation().getY()));
                    } else {
                        arwgraph.createGraphNode(((float)dwidth - descale(startDrag.x)),
                                descale(startDrag.y), GraphNodeType.SIMPLE);
                        start_node = arwgraph.getLastNode();
                    }


                    int x1 = startDrag.x;
                    int y1 = startDrag.y;
                    int x2 = e.getX();
                    int y2 = e.getY();
                    if (Math.abs(startDrag.x - e.getX()) < scale(SENSIBILITY)) {
                        x2 = x1;
                    }
                    if (Math.abs(y1 - y2) < scale(SENSIBILITY)) {
                        y2 = y1;
                    }
                    endDrag.x = x2;
                    endDrag.y = y2;
                    if (arwgraph.distancetoNeighborEdge(((float)dwidth - descale(endDrag.x)),
                            descale(endDrag.y)) < SENSIBILITY) {

                        //Insere um nó, se for necessário, na aresta mais próxima, se dentro da sensibilidade
                        //Dado que não é garantido que o nó seja o último, o id serve paar identificar o nó
                        //de início.
                        end_node = arwgraph.insertNode(new ARWGraphNode(arwgraph.getMaxIdNodes() + 1,
                                ((float)dwidth - descale(endDrag.x)),
                                descale(endDrag.y), GraphNodeType.SIMPLE));

                        endDrag = new Point(scale((float)dwidth - end_node.getLocation().getX()),
                                scale(end_node.getLocation().getY()));
                    } else {
                        arwgraph.createGraphNode(((float)dwidth - descale(endDrag.x)),
                                descale(endDrag.y), GraphNodeType.SIMPLE);
                        end_node = arwgraph.getLastNode();
                    }

                    arwgraph.makeNeighbors(start_node, end_node, false);

                    startDrag = null;
                    endDrag = null;
                    l.repaint();
                }
            }
            if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
                endDrag = new Point(e.getX(), e.getY());
                l.repaint();
            }

        }

        @Override
        public void paint(Graphics g, JComponent c) {

            int width = c.getWidth();

            AMPLIFY = Math.min(((float) c.getSize().width) / warehouse.getWidth(), ((float) c.getSize().height) / warehouse.getDepth());
            Graphics2D g2 = (Graphics2D) g.create();

            super.paint(g2, c);
            if (arwgraph != null) {
                for (ARWGraphNode node : arwgraph.getGraphNodes()) {
                    g2.setPaint(Color.BLACK);
                    if (node.contains_product())
                        g2.setPaint(Color.BLUE);
                    g2.drawOval(width - scale(node.getLocation().getX()) - (NODE_SIZE / 2),
                            scale(node.getLocation().getY()) - (NODE_SIZE / 2), NODE_SIZE, NODE_SIZE);
                    g2.drawString(node.printName(), width - scale(node.getLocation().getX()) + (NODE_SIZE),
                            scale(node.getLocation().getY()) - (NODE_SIZE));
                }
                for (Edge e : arwgraph.getEdges()) {
                    Shape r = makeLine(width - scale(e.getStart().getLocation().getX()), scale(e.getStart().getLocation().getY()),
                            width - scale(e.getEnd().getLocation().getX()), scale(e.getEnd().getLocation().getY()));
                    g2.setPaint(Color.DARK_GRAY);
                    g2.draw(r);
                }

                if (startDrag != null && endDrag != null) {
                    g2.setPaint(Color.DARK_GRAY);
                    Shape r = makeLine(startDrag.x, startDrag.y, endDrag.x, endDrag.y);
                    g2.draw(r);
                }
            }
        }

    }

}

//    public void paint(Graphics g, JComponent c) {
//
//        AMPLIFY=Math.min(((float)c.getSize().width)/warehouse.getWidth(),((float)c.getSize().height)/warehouse.getDepth());
//        Graphics2D g2 = (Graphics2D) g.create();
//
//        super.paint(g2, c);
//        if (arwgraph!=null) {
//            for (ARWGraphNode node : arwgraph.getGraphNodes()) {
//                g2.setPaint(Color.BLACK);
//                if (node.contains_product())
//                    g2.setPaint(Color.BLUE);
//                g2.drawOval(scale(warehouse.getWidth() - node.getLocation().getX()) - (NODE_SIZE / 2),
//                        scale(node.getLocation().getY()) - (NODE_SIZE / 2), NODE_SIZE, NODE_SIZE);
//                g2.drawString(node.printName(), scale(warehouse.getWidth() - node.getLocation().getX()) + (NODE_SIZE),
//                        scale(node.getLocation().getY()) - (NODE_SIZE));
//            }
//            for (Edge e : arwgraph.getEdges()) {
//                Shape r = makeLine(scale(warehouse.getWidth() - e.getStart().getLocation().getX()), scale(e.getStart().getLocation().getY()),
//                        scale(warehouse.getWidth() - e.getEnd().getLocation().getX()), scale(e.getEnd().getLocation().getY()));
//                g2.setPaint(Color.DARK_GRAY);
//                g2.draw(r);
//            }
//
//            if (startDrag != null && endDrag != null) {
//                g2.setPaint(Color.DARK_GRAY);
//                Shape r = makeLine(startDrag.x, startDrag.y, endDrag.x, endDrag.y);
//                g2.draw(r);
//            }
//        }
//    }

//    protected void processMouseEvent(MouseEvent e, JLayer l) {
//
//        int width = l.getWidth();
//        int x = e.getX();
//
//        System.out.println("Width: " + width);
//        System.out.println("X: " + x);
//
//        if (e.getID() == MouseEvent.MOUSE_PRESSED) {
//            startDrag = new Point(width - e.getX(), e.getY());
//            endDrag = startDrag;
//            if (node_action == Node_Action.REMOVE) {
//                ARWGraphNode node = arwgraph.findClosestNode(warehouse.getWidth() - descale(e.getX()),
//                        descale(e.getY()), (float) SENSIBILITY);
//                if (node != null) {
//                    arwgraph.removeNode(node);
//                }
//            }
//            l.repaint();
//
//        }
//
//        if (e.getID() == MouseEvent.MOUSE_RELEASED) {
//            if (!(startDrag.x == width - e.getX() && startDrag.y == e.getY() || node_action == Node_Action.REMOVE)) {
//
//                if (arwgraph.distancetoNeighborEdge((warehouse.getWidth() - descale(startDrag.x)),
//                        descale(startDrag.y)) < SENSIBILITY) {
//
//                    //Insere um nó, se for necessário, na aresta mais próxima, se dentro da sensibilidade
//                    //Dado que não é garantido que o nó seja o último, o id serve paar identificar o nó
//                    //de início.
//                    start_node = arwgraph.insertNode(
//                            new ARWGraphNode(
//                                    arwgraph.getMaxIdNodes() + 1,
//                                    warehouse.getWidth() - descale(startDrag.x),
//                                    descale(startDrag.y),
//                                    GraphNodeType.SIMPLE));
//                    startDrag = new Point(
//                            width - scale(start_node.getLocation().getX()),
//                            scale(start_node.getLocation().getY()));
//                } else {
//                    arwgraph.createGraphNode(
//                            warehouse.getWidth() - descale(startDrag.x),
//                            descale(startDrag.y),
//                            GraphNodeType.SIMPLE);
//                    start_node = arwgraph.getLastNode();
//                }
//
//                int x1 = startDrag.x;
//                int y1 = startDrag.y;
//                int x2 = width - e.getX();
//                int y2 = e.getY();
//                if (Math.abs(startDrag.x - width - e.getX()) < scale(SENSIBILITY)) {
//                    x2 = x1;
//                }
//                if (Math.abs(y1 - y2) < scale(SENSIBILITY)) {
//                    y2 = y1;
//                }
//                endDrag.x = x2;
//                endDrag.y = y2;
//                if (arwgraph.distancetoNeighborEdge((warehouse.getWidth() - descale(endDrag.x)),
//                        descale(endDrag.y)) < SENSIBILITY) {
//
//                    //Insere um nó, se for necessário, na aresta mais próxima, se dentro da sensibilidade
//                    //Dado que não é garantido que o nó seja o último, o id serve paar identificar o nó
//                    //de início.
//                    end_node = arwgraph.insertNode(
//                            new ARWGraphNode(
//                                    arwgraph.getMaxIdNodes() + 1,
//                                    warehouse.getWidth() - descale(endDrag.x),
//                                    descale(endDrag.y),
//                                    GraphNodeType.SIMPLE));
//
//                    endDrag = new Point(
//                            width - scale(end_node.getLocation().getX()),
//                            scale(end_node.getLocation().getY()));
//                } else {
//                    arwgraph.createGraphNode(
//                            warehouse.getWidth() - descale(endDrag.x),
//                            descale(endDrag.y),
//                            GraphNodeType.SIMPLE);
//                    end_node = arwgraph.getLastNode();
//                }
//
//                arwgraph.makeNeighbors(start_node, end_node, false);
//
//                startDrag = null;
//                endDrag = null;
//                l.repaint();
//            }
//        }
//        if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
//            endDrag = new Point(width - e.getX(), e.getY());
//            l.repaint();
//        }
//
//    }
