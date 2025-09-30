package services;

import Model.FileEntry;
import Model.ScanSummary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
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
            Instant start = Instant.now();
            LOGGER.info(String.format("Scanning directory: %s", userAbsolutePath));
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
            List<FileEntry> fileEntries = walk
                    .filter(Files::isRegularFile)
                    .map(this::addMetaData)
                    .filter(Objects::nonNull)
                    .toList();




            return fileEntries;
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

            return new FileEntry(relativePath, size, lastModifiedTime.toMillis());
        } catch (IOException e) {
            LOGGER.warning(String.format("Error while reading file %s: %s", filePath, e.getMessage()));
        }
        return null;
    }
}
