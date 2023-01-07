package io.github.poshjosh.ratelimiter;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SerializationTest<T extends Serializable> {

    private final Path dir;

    public SerializationTest() {
        this.dir = Paths.get(System.getProperty("java.io.tmpdir"));
        System.out.println(LocalDateTime.now() + " SerializationTest dir: " + dir);
    }

    protected T testSerialization(T input) {
        final String fileName = dir.resolve(input.getClass().getName() + ".ser").toString();
        final T output = testSerialization(input, fileName);
        assertEquals(input, output);
        return output;
    }

    private T testSerialization(T input, String fileName) throws AssertionError{
        try {
            serialize(input, fileName);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        try {
            return (T)deserialize(fileName);
        } catch (ClassNotFoundException | IOException e) {
            throw new AssertionError(e);
        }
    }

    private void serialize(Object obj, String fileName)throws IOException {
        try (FileOutputStream fos = new FileOutputStream(fileName, false)) {
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(obj);
        }
    }

    private Object deserialize(String fileName) throws IOException, ClassNotFoundException {
        try (FileInputStream fis = new FileInputStream(fileName)) {
            ObjectInputStream ois = new ObjectInputStream(fis);
            return ois.readObject();
        }
    }
}