package dev.crashteam.ke_data_scrapper.exception;

public class CategoryRequestException extends RuntimeException {

    public CategoryRequestException() {
    }

    public CategoryRequestException(String message) {
        super(message);
    }

    public CategoryRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
