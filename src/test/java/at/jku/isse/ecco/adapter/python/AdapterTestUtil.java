package at.jku.isse.ecco.adapter.python;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class AdapterTestUtil {

    public static boolean comparePythonFiles(Path p1, Path p2) {
        try {
            BufferedReader reader1 = new BufferedReader(new FileReader(p1.toFile()));
            BufferedReader reader2 = new BufferedReader(new FileReader(p2.toFile()));

            String pattern = "\s*";

            String line1 = reader1.readLine();
            String line2 = reader2.readLine();
            while (line1 != null || line2 != null) {
                if (line1 == null || line2 == null) {
                    return false;
                } else if (line1.matches(pattern) && line2.matches(pattern)) {
                    // continue - empty lines are normalized
                } else if (!line1.equalsIgnoreCase(line2)) {
                    return false;
                }

                line1 = reader1.readLine();
                line2 = reader2.readLine();
            }
        } catch (FileNotFoundException e) {
            System.out.print("File not found while checking " + p2.getFileName() + " ...");
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean compareJsonFiles(Path p1, Path p2) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            JsonNode actualObj1 = mapper.readTree(new String(Files.readAllBytes(p1)));
            JsonNode actualObj2 = mapper.readTree(new String(Files.readAllBytes(p2)));

            return actualObj1.equals(actualObj2);
        } catch (NoSuchFileException e) {
            System.out.print("File not found while checking " + p2.getFileName() + " ...");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean compareJupyterFiles(Path p1, Path p2) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            JsonNode actualObj1 = mapper.readTree(Files.readString(p1));
            JsonNode actualObj2 = mapper.readTree(Files.readString(p2));

            JupyterComparator cmp = new JupyterComparator(); // ignore empty line

            List<String> ignoreFields = Arrays.asList("outputs", "metadata", "execution_count");

            removeRecursively(actualObj1, ignoreFields);
            removeRecursively(actualObj2, ignoreFields);
            return actualObj1.equals(cmp, actualObj2);
        } catch (NoSuchFileException e) {
            System.out.print("File not found while checking " + p2.getFileName() + " ...");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void removeRecursively(JsonNode j, Collection<String> propertyNames) {
        if (j instanceof ObjectNode o) {
            o.remove(propertyNames);
            o.elements().forEachRemaining(e -> removeRecursively(e, propertyNames));
        }
        if (j instanceof ArrayNode a) {
            a.elements().forEachRemaining(e -> removeRecursively(e, propertyNames));
        }
    }

    private static class JupyterComparator implements Comparator<JsonNode> {
        private static final String pattern = "\s*\n*";

        @Override
        public int compare(JsonNode j1, JsonNode j2) {

            if ((j1 instanceof TextNode t1) && (j2 instanceof TextNode t2)) {
                if (t1.asText().matches(pattern) && t2.asText().matches(pattern)) {
                    return 0;
                }
            }

            if (j1.equals(j2)) {
                return 0;
            }
            return 1;
        }
    }

    /**
     * delete all files / folders of a directory (excluding root-folder)
     * source: <a href="https://howtodoinjava.com/java/io/delete-directory-recursively/">...</a>
     */
    public static void deleteDir(Path rootDir) {
        try {
            Files.walkFileTree(rootDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir,
                                                          IOException exc) throws IOException {
                    if (exc == null) {
                        if (dir != rootDir) Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    } else {
                        throw exc;
                    }
                }

            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
