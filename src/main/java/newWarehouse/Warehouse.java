package newWarehouse;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.List;

public class Warehouse {
    List<Prefab> prefabList;
    Hashtable<String, Point2D.Float> wmscodes;
    Point2D.Float defaultstart;
    Point2D.Float area;

    public Warehouse() {
        prefabList = new ArrayList<>();
        wmscodes = new Hashtable<>();
        defaultstart = new Point2D.Float();
        area = new Point2D.Float();
    }

    public void clear() {
        prefabList = new ArrayList<>();
        wmscodes = new Hashtable<>();
        defaultstart = new Point2D.Float();
        area = new Point2D.Float();
    }

    public boolean checkWms(String wmscode) {
        return wmscodes.containsKey(wmscode);
    }

    public Hashtable<String, Point2D.Float> getWmscodes() {
        return wmscodes;
    }

    public void createFromXML(String content) {
        clear();
        Hashtable<String, Point2D.Float> prefabs = new Hashtable<>();
        //System.out.println(content);
        DocumentBuilder db;
        try {
            db = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            InputSource is = new InputSource(new StringReader(content));

            Document doc;

            doc = db.parse(is);

            doc.getDocumentElement().normalize();
            NodeList nodes = doc.getElementsByTagName("config");
            if (nodes.item(0).getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) nodes.item(0);
                area.x = Float.parseFloat(element.getElementsByTagName("width").item(0).getTextContent());
                area.y = Float.parseFloat(element.getElementsByTagName("depth").item(0).getTextContent());

                element = (Element) nodes.item(0);
                defaultstart.x = Float.parseFloat(element.getElementsByTagName("positionX").item(0).getTextContent());
                defaultstart.y = Float.parseFloat(element.getElementsByTagName("positionY").item(0).getTextContent());
            }

            nodes = doc.getElementsByTagName("prefabs");
            Element element = (Element) nodes.item(0);
            nodes = element.getElementsByTagName("entry");
            for (int j = 0; j < nodes.getLength(); j++) {
                element = (Element) nodes.item(j);
                Point2D.Float prefabarea = new Point2D.Float();
                prefabarea.x = Float.parseFloat(element.getElementsByTagName("sizeX").item(0).getTextContent());
                prefabarea.y = Float.parseFloat(element.getElementsByTagName("sizeY").item(0).getTextContent());
                if (element.getElementsByTagName("ID").getLength() > 0)
                    prefabs.put(element.getElementsByTagName("ID").item(0).getTextContent(), prefabarea);
            }

            nodes = doc.getElementsByTagName("racks");
            element = (Element) nodes.item(0);
            nodes = element.getElementsByTagName("entry");
            for (int j = 0; j < nodes.getLength(); j++) {
                element = (Element) nodes.item(j);
                Prefab prefab = new Prefab();
                prefab.type = Prefabtype.RACK;
                if (element.getElementsByTagName("prefabID").getLength() > 0) {
                    prefab.area.x = Float.parseFloat(element.getElementsByTagName("positionX").item(0).getTextContent());
                    prefab.area.y = Float.parseFloat(element.getElementsByTagName("positionY").item(0).getTextContent());
                    String prefabid = element.getElementsByTagName("prefabID").item(0).getTextContent();
                    prefab.area.width = prefabs.get(prefabid).x;
                    prefab.area.height = prefabs.get(prefabid).y;
                    prefab.rotation = Float.parseFloat(element.getElementsByTagName("rotationZ").item(0).getTextContent());
                    prefabList.add(prefab);
                    //Falta wmscodes
                    Point2D.Float position = new Point2D.Float();
                    position.x = prefab.area.x;
                    position.y = prefab.area.y;
                    NodeList wmsCodes = element.getElementsByTagName("wmsCode");
                    for (int k = 0; k < wmsCodes.getLength(); k++) {
                        wmscodes.put((wmsCodes.item(k)).getTextContent(), position);
                    }
                }
            }

            nodes = doc.getElementsByTagName("structures");
            element = (Element) nodes.item(0);
            nodes = element.getElementsByTagName("entry");
            for (int j = 0; j < nodes.getLength(); j++) {
                element = (Element) nodes.item(j);
                Prefab prefab = new Prefab();
                prefab.type = Prefabtype.STRUCTURE;
                prefab.area.x = Float.parseFloat(element.getElementsByTagName("positionX").item(0).getTextContent());
                prefab.area.y = Float.parseFloat(element.getElementsByTagName("positionY").item(0).getTextContent());
                String prefabid = element.getElementsByTagName("prefabID").item(0).getTextContent();
                prefab.area.width = prefabs.get(prefabid).x;
                prefab.area.height = prefabs.get(prefabid).y;
                prefab.rotation = Float.parseFloat(element.getElementsByTagName("rotationZ").item(0).getTextContent());
                prefabList.add(prefab);
                //Falta wmscodes
            }

            nodes = doc.getElementsByTagName("floorAreas");
            if (nodes.getLength() > 0) {
                element = (Element) nodes.item(0);
                nodes = element.getElementsByTagName("entry");
                for (int j = 0; j < nodes.getLength(); j++) {
                    element = (Element) nodes.item(j);
                    Prefab prefab = new Prefab();
                    prefab.type = Prefabtype.FLOORAREA;
                    prefab.area.x = Float.parseFloat(element.getElementsByTagName("positionX").item(0).getTextContent());
                    prefab.area.y = Float.parseFloat(element.getElementsByTagName("positionY").item(0).getTextContent());
                    String prefabid = element.getElementsByTagName("prefabID").item(0).getTextContent();
                    prefab.area.width = prefabs.get(prefabid).x;
                    prefab.area.height = prefabs.get(prefabid).y;
                    prefab.rotation = Float.parseFloat(element.getElementsByTagName("rotationZ").item(0).getTextContent());
                    prefabList.add(prefab);
                    Point2D.Float position = new Point2D.Float();
                    position.x = prefab.area.x;
                    position.y = prefab.area.y;
                    wmscodes.put((element.getElementsByTagName("wmsCode").item(0)).getTextContent(), position);
                }
            }

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    public List<Prefab> listAreas() {
        return prefabList;
    }

    public Point2D.Float getWms(String wmscode) {
        return wmscodes.get(wmscode);
    }

    public Point2D.Float getArea() {
        return area;
    }

    public float getWidth() {
        return area.x;
    }

    public float getDepth() {
        return area.y;
    }

    public List<Prefab> getPrefabList() {
        return prefabList;
    }

    public Point2D.Float getDefaultstart() {
        return defaultstart;
    }

    public void Print() {
        //Para debug
        System.out.println(prefabList.size() + " Prefabs");
        for (Prefab prefab : prefabList) {
            System.out.println(prefab.type + " larg:" + prefab.area);
        }

        System.out.println(wmscodes.size() + " wmscodes");

        for (String wmscode : wmscodes.keySet()) {
            System.out.println(wmscode + "@:" + wmscodes.get(wmscode).x + ";" + wmscodes.get(wmscode).y);
        }

        System.out.println("Default start @:" + defaultstart.x + ";" + defaultstart.y);
        System.out.println("Area:" + area.x + ";" + area.y);
    }

    public ArrayList<Shape> generateShapes(float expandby) {

        ArrayList<Shape> shapes = new ArrayList<>();
        for (Prefab prefab : getPrefabList()) {
            Rectangle2D.Float rec = (Rectangle2D.Float) prefab.area.clone();
            AffineTransform tx = new AffineTransform();

            double multx = (prefab.area.width + expandby) / prefab.area.width;
            double multy = (prefab.area.height + expandby) / prefab.area.height;
            rec.width *= multx;
            rec.height *= multy;
            rec.x -= expandby / 2;
            rec.y -= expandby / 2;

            tx.rotate(Math.toRadians(-prefab.rotation), prefab.area.x, prefab.area.y);

            Shape newShape = tx.createTransformedShape(rec);
            shapes.add(newShape);

        }

        return shapes;
    }


    public ArrayList<Point2D.Float> generateIntersections(float corridorwidth) {
        ArrayList<Shape> shapes = generateShapes(corridorwidth);
        ArrayList<Point2D.Float> pontos = new ArrayList<>();
        Rectangle2D.Float walls = new Rectangle2D.Float(0, 0, getArea().x, getArea().y);

        pontos.add((Point2D.Float) defaultstart.clone());
        //Gera pontos em todos os vértices das áreas expandidas
        for (int i = 0; i < shapes.size(); i++) {

            Rectangle2D recti = shapes.get(i).getBounds2D();
            pontos.add(new Point2D.Float((float) recti.getMinX(), (float) recti.getMinY()));
            pontos.add(new Point2D.Float((float) recti.getMinX(), (float) recti.getMaxY()));
            pontos.add(new Point2D.Float((float) recti.getMaxX(), (float) recti.getMinY()));
            pontos.add(new Point2D.Float((float) recti.getMaxX(), (float) recti.getMaxY()));
            if (walls.intersects(recti.getBounds2D())) {
                //E na intersecção das áreas expandidas com as paredes
                Rectangle2D rect = walls.createIntersection(recti);
                pontos.add(new Point2D.Float((float) rect.getMinX(), (float) rect.getMinY()));
                pontos.add(new Point2D.Float((float) rect.getMinX(), (float) rect.getMaxY()));
                pontos.add(new Point2D.Float((float) rect.getMaxX(), (float) rect.getMinY()));
                pontos.add(new Point2D.Float((float) rect.getMaxX(), (float) rect.getMaxY()));

            }
            for (int j = i + 1; j < shapes.size(); j++) {
                //E na intersecção de cada área expandida com as restantes
                Rectangle2D rectj = shapes.get(j).getBounds2D();
                if (recti.intersects(rectj)) {
                    Rectangle2D rect = recti.createIntersection(rectj);
                    pontos.add(new Point2D.Float((float) rect.getMinX(), (float) rect.getMinY()));
                    pontos.add(new Point2D.Float((float) rect.getMinX(), (float) rect.getMaxY()));
                    pontos.add(new Point2D.Float((float) rect.getMaxX(), (float) rect.getMinY()));
                    pontos.add(new Point2D.Float((float) rect.getMaxX(), (float) rect.getMaxY()));
                }
            }

        }

        //Elimina pontos redundantes
        Set<Point2D.Float> conjunto = new LinkedHashSet<>(pontos);
        pontos.clear();
        pontos.addAll(conjunto);

        //Elimina todos os pontos gerados em zonas proibidas
        shapes = generateShapes(corridorwidth / 2);
        for (Shape shape : shapes) {
            Rectangle2D recti = shape.getBounds2D();
            for (int j = 0; j < pontos.size(); j++) {
                Point2D.Float ponto = pontos.get(j);
                if ((ponto.x > recti.getMinX()) && (ponto.x < recti.getMaxX()) &&
                        (ponto.y > recti.getMinY()) && (ponto.y < recti.getMaxY())) {
                    pontos.remove(j);
                    j--;
                }
            }
        }

        //Elimina todos os pontos gerados fora da sala
        for (int j = 0; j < pontos.size(); j++) {
            Point2D.Float ponto = pontos.get(j);
            if (ponto.x < walls.getMinX() || ponto.x > walls.getMaxX() ||
                    ponto.y < walls.getMinY() || ponto.y > walls.getMaxY()) {
                pontos.remove(j);
                j--;
            }
        }

        //Funde os pontos demasiado próximos, gerando um único ponto com coordenadas intermédias.
        for (int i = 0; i < pontos.size(); i++)
            for (int j = i + 1; j < pontos.size(); j++) {
                if (pontos.get(i).distance(pontos.get(j)) < corridorwidth / 2) {
                    pontos.get(i).x = (pontos.get(i).x + pontos.get(j).x) / 2;
                    pontos.get(i).y = (pontos.get(i).y + pontos.get(j).y) / 2;
                    pontos.remove(j);
                    j--;
                }
            }

        return pontos;
    }

    static class LineComparator implements Comparator<Line2D> {
        @Override
        public int compare(Line2D o1, Line2D o2) {

            Float d1 = (float) o1.getP1().distance(o1.getP2());
            Float d2 = (float) o2.getP1().distance(o2.getP2());

            return d1.compareTo(d2);
        }
    }

    public boolean cruzam(Line2D.Float l1, Line2D.Float l2) {

        if (l1.intersectsLine(l2)) {
            return !(Math.abs(l1.x1 - l2.x1) < 0.01 && Math.abs(l2.y1 - l1.y1) < 0.01 ||
                    Math.abs(l1.x2 - l2.x1) < 0.01 && Math.abs(l1.y2 - l2.y1) < 0.01 ||
                    Math.abs(l1.x1 - l2.x2) < 0.01 && Math.abs(l1.y1 - l2.y2) < 0.01 ||
                    Math.abs(l1.x2 - l2.x2) < 0.01 && Math.abs(l1.y2 - l2.y2) < 0.01);
        } else
            return false;
    }

    public boolean quaseparalelas(Line2D.Float l1, Line2D.Float l2) {

        double cosx = (((l2.y2 - l2.y1) * (l1.y2 - l1.y1)) + ((l2.x2 - l2.x1) * (l1.x2 - l1.x1))) /
                (l2.getP1().distance(l2.getP2()) * l1.getP1().distance(l1.getP2()));

        if (cosx > 0.985) {
            Point2D comum, ext1, ext2;

            if (l1.getP1().equals(l2.getP1())) {
                comum = l1.getP1();
                ext1 = l1.getP2();
                ext2 = l2.getP2();
            } else if (l1.getP1().equals(l2.getP2())) {
                comum = l1.getP1();
                ext1 = l1.getP2();
                ext2 = l2.getP1();
            } else if (l1.getP2().equals(l2.getP1())) {
                comum = l1.getP2();
                ext1 = l1.getP1();
                ext2 = l2.getP2();
            } else if (l1.getP2().equals(l2.getP2())) {
                comum = l1.getP2();
                ext1 = l1.getP1();
                ext2 = l2.getP1();
            } else
                return false;
            return (ext1.distance(ext2) < Math.max(comum.distance(ext1), comum.distance(ext2)));
        } else
            return false;

    }

    public ArrayList<Line2D.Float> createClearPaths(float corridorwidth) {
        ArrayList<Point2D.Float> pontos = generateIntersections(corridorwidth);
        ArrayList<Shape> shapes = generateShapes(corridorwidth / 2);
        ArrayList<Line2D.Float> linhas = new ArrayList<>();

        for (int i = 0; i < pontos.size(); i++) {
            for (int j = i + 1; j < pontos.size(); j++) {
                Line2D.Float linha = new Line2D.Float(pontos.get(i), pontos.get(j));
                boolean intersetou = false;
                for (Shape shape : shapes) {
                    if (linha.intersects(shape.getBounds2D())) {
                        intersetou = true;
                        break;
                    }
                }
                if (!intersetou)
                    linhas.add(linha);
            }

        }
        linhas.sort(new LineComparator());

        ArrayList<Line2D.Float> selectedlines = new ArrayList<>();
        selectedlines.add(linhas.get(0));

        for (int i = 1; i < linhas.size(); i++) {
            boolean intersetou = false;
            Line2D.Float linhatual = linhas.get(i);
            for (Line2D.Float selectedline : selectedlines) {
                if (cruzam(linhatual, selectedline) || quaseparalelas(linhatual, selectedline)) {
                    intersetou = true;
                    break;
                }
            }
            if (!intersetou)
                selectedlines.add(linhatual);
        }
        return selectedlines;
    }


    public void geraWarehouse(float comprimento, float largura, float largcorredor, float largala,
                              int numalas, int numcorredores, float largwms) {
        clear();

        //Hashtable<String, Point2D.Float> wmscodes;
        defaultstart.x = comprimento - largcorredor / 2;
        defaultstart.y = largura - largala / 2;
        area.x = comprimento;
        area.y = largura;

        numalas = Math.min(numalas, (int) (largura / largala / 3));
        numcorredores = Math.min(numcorredores, (int) (comprimento / largcorredor / 2 - 1));

        ArrayList<Float> centroalas = new ArrayList<>();
        float incremento = 0;

        centroalas.add(defaultstart.y);
        if (numalas >= 2) {
            centroalas.add(largala / 2);
            incremento = (centroalas.get(0) - centroalas.get(1)) / (numalas - 1);
        }
        for (int alas = 2; alas < numalas; alas++)
            centroalas.add(centroalas.get(alas - 1) + incremento);

        ArrayList<Float> centrocorr = new ArrayList<>();

        incremento = Math.min(largala, (comprimento - (2 * numcorredores - 1) * largcorredor) / 2);

        centrocorr.add(comprimento - incremento);
        centrocorr.add(incremento + largcorredor);

        incremento = 2 * largcorredor;
        for (int corrs = 2; corrs < numcorredores; corrs++) {
            if ((corrs % 2) == 0)
                centrocorr.add(centrocorr.get(corrs - 2) - incremento);
            else
                centrocorr.add(centrocorr.get(corrs - 2) + incremento);
        }
        centrocorr.sort(Comparator.naturalOrder());
        centroalas.sort(Comparator.naturalOrder());
        int numwms = 0;
        for (int alas = 0; alas < numalas; alas++) {

            for (int corrs = 0; corrs < numcorredores; corrs++) {
                Prefab rack = new Prefab();
                rack.type = Prefabtype.RACK;
                if (numalas == 1) {
                    rack.area.y = 0;
                    rack.area.height = centroalas.get(0) - largala / 2;
                } else {
                    rack.area.y = centroalas.get(alas) + largala / 2;
                    if (alas < numalas - 1)
                        rack.area.height = centroalas.get(alas + 1) - centroalas.get(alas) - largala;
                    else
                        rack.area.height = 0;
                }
                rack.area.x = centrocorr.get(corrs) - 3 / 2 * largcorredor;
                rack.area.width = largcorredor;
                rack.rotation = 0f;
                if (rack.area.height > 0) {
                    prefabList.add(rack);
                    for (int wms = 0; wms < rack.area.height / largwms; wms++) {
                        float y = rack.area.y + wms * rack.area.height / largwms;
                        String s = String.format("wms%06d", ++numwms);
                        wmscodes.put(s, new Point2D.Float(rack.area.x, y));
                        s = String.format("wms%06d", ++numwms);
                        wmscodes.put(s, new Point2D.Float(rack.area.x + rack.area.width, y));
                    }
                }
            }

        }

        if (numalas > 1)
            wmscodes.put("Destiny", new Point2D.Float(largala / 2, largala / 2));
        else
            wmscodes.put("Destiny", new Point2D.Float(largala / 2, centroalas.get(0)));
    }

}
