package dev.crashteam.ke_data_scrapper.mapper;

public class ProductCorruptedException extends RuntimeException {
    public ProductCorruptedException(String message) {
        super(message);
    }
}
