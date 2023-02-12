package dev.crashteam.ke_data_scrapper.exception;

public class KeGqlRequestException extends RuntimeException {

    public KeGqlRequestException() {
        super();
    }

    public KeGqlRequestException(String message) {
        super(message);
    }

    public KeGqlRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
