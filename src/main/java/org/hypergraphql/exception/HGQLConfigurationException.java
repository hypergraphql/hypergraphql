package org.hypergraphql.exception;

public class HGQLConfigurationException extends IllegalArgumentException {

    public HGQLConfigurationException(String message) {
        super(message);
    }

    public HGQLConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
