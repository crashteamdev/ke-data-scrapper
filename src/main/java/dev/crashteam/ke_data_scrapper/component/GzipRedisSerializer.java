package dev.crashteam.ke_data_scrapper.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GzipRedisSerializer implements RedisSerializer<Object> {

    private final ObjectMapper objectMapper;

    public GzipRedisSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Object deserialize(byte[] bytes) {
        return deserializeFromBytes(decompress(bytes));
    }

    @Override
    public byte[] serialize(Object object) {
        return compress(serializeToBytes(object));
    }

    private Object deserializeFromBytes(byte[] bytes) {
        try {
            return objectMapper.readValue(bytes, Object.class);
        } catch (IOException e) {
            throw new RuntimeException("Unable to deserialize from bytes", e);
        }
    }

    private byte[] serializeToBytes(Object object) {
        try {
            return objectMapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to write object as bytes", e);
        }
    }

    private byte[] compress(byte[] content) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(content);
            gzipOutputStream.finish();
            gzipOutputStream.flush();
        } catch (IOException e) {
            throw new SerializationException("Unable to compress data", e);
        }
        return byteArrayOutputStream.toByteArray();
    }

    private byte[] decompress(byte[] contentBytes) {
        try (InputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(contentBytes))) {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                inputStream.transferTo(outputStream);
                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            throw new SerializationException("Unable to decompress data", e);
        }
    }
}
