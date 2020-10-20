package org.hypergraphql.exception;

public class HGQLConfigurationException extends IllegalArgumentException {

    public HGQLConfigurationException(final String message) {
        super(message);
    }

    public HGQLConfigurationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
