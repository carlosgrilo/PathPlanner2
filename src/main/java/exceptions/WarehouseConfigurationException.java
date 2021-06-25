package exceptions;

public class WarehouseConfigurationException extends Exception {

    public WarehouseConfigurationException() {
    }

    public WarehouseConfigurationException(String msg) {
        super(msg);
    }
}