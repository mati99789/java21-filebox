package services;

import model.FileEntry;
import model.ScanSummary;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class ScannerService {
    private final Path userAbsolutePath;
    private final String outputPath;
    private final static Logger LOGGER = Logger.getLogger(ScannerService.class.getName());

    public ScannerService(String path, String outputPath) {
        this.outputPath = outputPath;
        Path convertToPath = Path.of(path);
        Path absolutePath = convertToPath.toAbsolutePath().normalize();

        checkPath(absolutePath);
        this.userAbsolutePath = absolutePath;
    }

    public ScanSummary scan() {
        if (Files.isDirectory(userAbsolutePath)) {
            LOGGER.info(String.format("Scanning directory: %s", userAbsolutePath));

            Instant start = Instant.now();
            List<FileEntry> files = exploreDirectory(userAbsolutePath);
            Instant end = Instant.now();
            long duration = end.toEpochMilli() - start.toEpochMilli();

            long totalBytes = files.stream().mapToLong(FileEntry::size).sum();
            long totalFiles = files.size();

            new IndexStoreService(new ScanSummary(userAbsolutePath.toString(), totalBytes, totalFiles, duration), outputPath).save(files);
            return new ScanSummary(userAbsolutePath.toString(), totalBytes, totalFiles, duration);

        } else {
            LOGGER.info(String.format("Scanning file: %s", userAbsolutePath));
        }
        return null;
    }

    private void checkPath(Path path) {
        if (!Files.exists(path)) {
            LOGGER.warning("Path does not exist");
            LOGGER.warning("Current directory: " + System.getProperty("user.dir"));
            System.exit(1);
        }
    }

    private List<FileEntry> exploreDirectory(Path path) {
        LOGGER.info(String.format("Exploring directory: %s", path));
        try (Stream<Path> walk = Files.walk(path)) {
            return walk
                    .filter(Files::isRegularFile)
                    .map(this::addMetaData)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            LOGGER.warning(String.format("Error while exploring directory %s: %s", path, e.getMessage()));
            System.exit(2);
        }
        return null;
    }

    private FileEntry addMetaData(Path filePath) {
        try {
            long size = Files.size(filePath);
            FileTime lastModifiedTime = Files.getLastModifiedTime(filePath);
            Path relativePath = this.userAbsolutePath.relativize(filePath).normalize();
            String sha256 = calculateSHA256(filePath);

            return new FileEntry(relativePath, size, lastModifiedTime.toMillis(), sha256);
        } catch (IOException e) {
            LOGGER.warning(String.format("Error while reading file %s: %s", filePath, e.getMessage()));
        }
        return null;
    }

    private String calculateSHA256(Path filePath) {
        try(BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(filePath))) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }

    }
}
