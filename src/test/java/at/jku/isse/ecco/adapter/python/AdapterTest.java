package at.jku.isse.ecco.adapter.python;

import at.jku.isse.ecco.storage.mem.dao.MemEntityFactory;
import at.jku.isse.ecco.tree.Node;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static at.jku.isse.ecco.adapter.python.AdapterTestUtil.*;


public class AdapterTest {

    static Path readPath;
    static Path writePath;

    final static String python = ".py";
    final static String jupyter = "ipynb";
    final static String json = "json";

    static PythonReader reader;
    static PythonWriter writer;

    @BeforeAll
    static void start() {
        reader = new PythonReader(new MemEntityFactory());
        writer = new PythonWriter();

        Path cwd = Paths.get("").toAbsolutePath();

        readPath = cwd.resolve("src/test/resources/read/").toAbsolutePath();
        writePath = cwd.resolve("src/test/resources/write/").toAbsolutePath();

        deleteDir(writePath);
    }

    @AfterAll
    static void calculateMetricsForWholeTestSet() {
        reader = null;
        writer = null;
    }

    @Test(groups = {"test"})
    @ParameterizedTest
    @MethodSource("filesPython")
    void testPython(final Path path) {
        testFiles(path);
    }

    @Test(groups = {"test"})
    @ParameterizedTest
    @MethodSource("filesJupyter")
    void testJupyter(final Path path) {
        testFiles(path);
    }

    @Test(groups = {"test"})
    @ParameterizedTest
    @MethodSource("filesJson")
    void testJson(final Path path) {
        testFiles(path);
    }


    void testFiles(final Path path) {

        Set<Node.Op> readNodes = reader.read(readPath, new Path[]{path});
        Set<Node> writeNodes = new HashSet<>(readNodes);
        writer.write(writePath, writeNodes);

        Path outputPath = readPath.relativize(path);
        outputPath = writePath.resolve(outputPath);

        boolean identical = false;
        if (path.toString().endsWith(python)) {
            identical = comparePythonFiles(path, outputPath);
        } else if (path.toString().endsWith(json)) {
            identical = compareJsonFiles(path, outputPath);
        } else if (path.toString().endsWith(jupyter)) {
            identical = compareJupyterFiles(path, outputPath);
        } else {
            System.out.println("Unknown File extension");
        }

        Assertions.assertTrue(identical);
    }

    public static Path[] filesPython() {
        return files(python);
    }

    public static Path[] filesJupyter() {
        return files(jupyter);
    }

    public static Path[] filesJson() {
        return files(json);
    }

    public static Path[] files(String type) {
        List<Path> files;

        //final String[] excludedFolders = new String[]{"yolov5-master", "ass1", "ass2"};
        final String[] excludedFolders = new String[] {};

        try (Stream<Path> s1 = Files.walk(readPath);
             Stream<Path> s2 = Files.walk(readPath)) {

            s1.filter(Files::isDirectory).map(in -> writePath.resolve(readPath.relativize(in)))
                    .forEach(out -> {
                                try {
                                    Files.createDirectories(out);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                    );

            files = s2.filter(f -> !Files.isDirectory(f))
                    .filter(f -> Arrays.stream(excludedFolders).noneMatch(s -> f.toAbsolutePath().toString().contains(s)))
                    .filter(f -> f.getFileName().toString().endsWith(type)).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return files.toArray(new Path[0]);
    }

}
