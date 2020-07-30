package communication;

public class Operator {
    private boolean available = false;
    private float x;
    private float y;
    private float z;
    private float q; //rotação
    private float dX;
    private float dY;
    private float dZ;
    private float dQ;
    private String id;
    private String estadoTarefa;

    public Operator(boolean available) {
        this.available = available;
    }

    public Operator(String id, boolean disponivel) {
        this.id = id;
        this.available = disponivel;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public float getQ() {
        return q;
    }

    public void setQ(float q) {
        this.q = q;
    }

    public float getdX() {
        return dX;
    }

    public void setdX(float dX) {
        this.dX = dX;
    }

    public float getdY() {
        return dY;
    }

    public void setdY(float dY) {
        this.dY = dY;
    }

    public float getdZ() {
        return dZ;
    }

    public void setdZ(float dZ) {
        this.dZ = dZ;
    }

    public float getdQ() {
        return dQ;
    }

    public void setdQ(float dQ) {
        this.dQ = dQ;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEstadoTarefa() {
        return estadoTarefa;
    }

    public void setEstadoTarefa(String estadoTarefa) {
        this.estadoTarefa = estadoTarefa;
    }
}