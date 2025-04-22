package dev.crashteam.ke_data_scrapper.component;

import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GzipRedisSerializer extends JdkSerializationRedisSerializer {

    @Override
    public Object deserialize(byte[] bytes) {
        return super.deserialize(decompress(bytes));
    }

    @Override
    public byte[] serialize(Object object) {
        return compress(super.serialize(object));
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
