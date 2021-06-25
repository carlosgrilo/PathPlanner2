package arwstate;

import whgraph.ARWGraphNode;

public class Pick {
    private String id;
    private String orderID;
    private String orderLine;
    private String quantity;
    private String origin;  // nome da área de origem do pick
    private String destiny; // nome da área de destino do pick
    private ARWGraphNode node;

    public Pick(
            String orderID,
            String orderLine,
            String id,
            String quantity,
            String origin,
            String destiny) {
        this.orderID = orderID;
        this.orderLine = orderLine;
        this.id = id;
        this.quantity = quantity;
        this.origin = origin;
        this.destiny = destiny;
    }

    /*
    List<String> products;
    <Ordem>Encomenda 199749</Ordem>
    <LinhaOrdem>1</LinhaOrdem>
    <Produto>50002020200050A</Produto>
    <Quantidade>4</Quantidade>
    <Origem>13.A.0.08</Origem>
    <Destino>Saída</Destino>
     */

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderID() {
        return orderID;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    public String getOrderLine() {
        return orderLine;
    }

    public void setOrderLine(String orderLine) {
        this.orderLine = orderLine;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestiny() {
        return destiny;
    }

    public void setDestiny(String destiny) {
        this.destiny = destiny;
    }

    public ARWGraphNode getNode() {
        return node;
    }

    public void setNode(ARWGraphNode node) {
        this.node = node;
    }

    public String toString(){
        String xmlString =
                "<Tarefa>\n"
                + "\t<Ordem>" + orderID + "</Ordem>\n"
                + "\t<LinhaOrdem>" + orderLine + "</LinhaOrdem>\n"
                + "\t<Produto>" + id + "</Produto>\n"
                + "\t<Quantidade>" + quantity + "</Quantidade>\n"
                + "\t<Origem>"+ origin +"</Origem>\n"
                + "\t<Destino>" + destiny + "</Destino>\n"
                + "</Tarefa>\n";
        return xmlString;
    }
}
