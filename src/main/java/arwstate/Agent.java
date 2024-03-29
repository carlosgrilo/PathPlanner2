package arwstate;

public class Agent {
    private String id;
    private String tagid;
    private float initialX;
    private float initialY;
    private String startNode;
    private String endNode;
    private Task task;

    public Agent(String id, float initialX, float initialY) {
        this.id = id;
        this.initialX = initialX;
        this.initialY = initialY;

    }
    public Agent(String id, float initialX, float initialY, String tagid) {
        this.id = id;
        this.initialX = initialX;
        this.initialY = initialY;
        this.tagid=tagid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTagid(String tagid) {
        this.tagid = tagid;
    }

    public String getTagid() {
        return tagid;
    }

    public float getInitialX() {
        return initialX;
    }

    public void setInitialX(float initialX) {
        this.initialX = initialX;
    }

    public float getInitialY() {
        return initialY;
    }

    public void setInitialY(float initialY) {
        this.initialY = initialY;
    }

    public String getStartNode() {
        return startNode;
    }

    public void setStartNode(String startNode) {
        this.startNode = startNode;
    }

    public String getEndNode() {
        return endNode;
    }

    public void setEndNode(String endNode) {
        this.endNode = endNode;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }
}
