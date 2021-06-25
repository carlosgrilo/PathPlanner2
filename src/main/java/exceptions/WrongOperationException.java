package exceptions;

public class WrongOperationException  extends Exception {

    public WrongOperationException() {
    }

    public WrongOperationException(String msg) {
        super(msg);
    }
}
