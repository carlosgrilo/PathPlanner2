package arwstate;

public class TaskOneAgentOneDestiny extends Task{

    private String destiny;

    public TaskOneAgentOneDestiny(Request request, String destiny) {
        super(request);
        this.destiny = destiny;
    }

    public String getDestiny() {
        return destiny;
    }
}
