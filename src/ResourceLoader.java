import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

final class ResourceLoader {
    private static final ClassLoader CL = ResourceLoader.class.getClassLoader();
    private static final String RESOURCE_PREFIX = "resources/";

    private ResourceLoader() {
    }

    static InputStream openStream(String path) throws IOException {
        if (path == null || path.isEmpty()) {
            throw new FileNotFoundException("Resource path is empty");
        }
        String normalized = normalize(path);
        InputStream fromClasspath = CL.getResourceAsStream(normalized);
        if (fromClasspath == null) {
            String fallback = stripPrefix(normalized);
            if (fallback != null) {
                fromClasspath = CL.getResourceAsStream(fallback);
            }
        }
        if (fromClasspath != null) {
            return fromClasspath;
        }
        Path filePath = Paths.get(path);
        if (Files.exists(filePath)) {
            return new FileInputStream(filePath.toFile());
        }
        throw new FileNotFoundException("Resource not found: " + path);
    }

    static boolean exists(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        String normalized = normalize(path);
        URL url = CL.getResource(normalized);
        if (url == null) {
            String fallback = stripPrefix(normalized);
            if (fallback != null) {
                url = CL.getResource(fallback);
            }
        }
        if (url != null) {
            return true;
        }
        try {
            return Files.exists(Paths.get(path));
        } catch (Exception ignored) {
            return false;
        }
    }

    static BufferedImage loadImage(String path) throws IOException {
        try (InputStream stream = openStream(path)) {
            BufferedImage image = ImageIO.read(stream);
            if (image == null) {
                throw new IOException("ImageIO.read returned null for " + path);
            }
            return image;
        }
    }

    static URL getResourceUrl(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        String normalized = normalize(path);
        URL url = CL.getResource(normalized);
        if (url != null) {
            return url;
        }
        String fallback = stripPrefix(normalized);
        if (fallback != null) {
            return CL.getResource(fallback);
        }
        return null;
    }

    static File materializeToTempFile(String path, String suffix) throws IOException {
        Path temp = Files.createTempFile("resource-", suffix != null ? suffix : ".tmp");
        temp.toFile().deleteOnExit();
        try (InputStream in = openStream(path)) {
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
        }
        return temp.toFile();
    }

    static String resolve(String basePath, String relative) {
        if (relative == null || relative.isEmpty()) {
            return normalize(relative);
        }
        String normalizedRelative = normalize(relative);
        Path relativePath = Paths.get(normalizedRelative);
        if (relativePath.isAbsolute()) {
            return normalizedRelative;
        }
        if (basePath == null || basePath.isEmpty()) {
            return normalizedRelative;
        }
        Path base = Paths.get(normalize(basePath)).getParent();
        if (base == null) {
            return normalizedRelative;
        }
        return normalize(base.resolve(relativePath).toString());
    }

    static String normalize(String path) {
        if (path == null) {
            return null;
        }
        String normalized = path.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static String stripPrefix(String path) {
        if (path == null) {
            return null;
        }
        if (path.startsWith(RESOURCE_PREFIX)) {
            String stripped = path.substring(RESOURCE_PREFIX.length());
            while (stripped.startsWith("/")) {
                stripped = stripped.substring(1);
            }
            return stripped.isEmpty() ? null : stripped;
        }
        return null;
    }
}
