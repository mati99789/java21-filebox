package services;

import model.FileEntry;
import model.IndexData;
import model.ScanSummary;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class IndexStoreService {
    private final static Logger LOGGER = Logger.getLogger(IndexStoreService.class.getName());
    private final ScanSummary scanSummary;
    private final String outputPath;


    public IndexStoreService(ScanSummary scanSummary, String outputPath) {
        this.scanSummary = scanSummary;
        this.outputPath = outputPath;
    }

    public void save(List<FileEntry> fileEntryList) {
        Path outputFil = Path.of(outputPath).toAbsolutePath().normalize();

        if (outputFil.endsWith("/") || outputFil.endsWith("\\")) {
            outputFil = outputFil.resolve("index.txt");
        }

        if (Files.isDirectory(outputFil) && Files.exists(outputFil)) {
            outputFil = outputFil.resolve("index.txt");
        }

        Path parentDir = outputFil.getParent();
        if (parentDir != null) {
            try {
                Files.createDirectories(parentDir);
            } catch (IOException e) {
                LOGGER.warning(String.format("Error while creating directory %s: %s", parentDir, e.getMessage()));
                System.exit(2);
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(outputFil, StandardCharsets.UTF_8)) {
            writer.write("# FileBox Index v1.0");
            writer.newLine();

            writer.write("# rootDir: " + scanSummary.rootPath());
            writer.newLine();

            writer.write("# generatedAt: " + Instant.now().toString());
            writer.newLine();

            writer.write("# fields=relativePath\tsize\tmtimeMillis\tsha256");
            writer.newLine();
            writer.newLine();
            for (FileEntry fileEntry : fileEntryList) {
                writer.write(String.format("%s\t%d\t%d\t%s", fileEntry.relativePath(), fileEntry.size(), fileEntry.lastModifiedTime(), fileEntry.sha256()));
                writer.newLine();
            }
            writer.write("======================");
            writer.newLine();
            writer.write(String.format("# Summary: files=%d, totalBytes=%d durationMillis=%d ms", scanSummary.totalFiles(), scanSummary.totalBytes(), scanSummary.durationMillis()));
        } catch (IOException e) {
            LOGGER.warning(String.format("Error while writing index file: %s", e.getMessage()));
            System.exit(2);
        }
    }

    public static IndexData load(Path path) {
        String rootPath = null;
        List<FileEntry> entries = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("# rootDir:")) {
                    rootPath = line.substring("# rootDir:".length()).trim();
                    continue;
                }

                if (line.startsWith("#") || line.startsWith("=") || line.trim().isEmpty()) {
                    continue;
                }

                FileEntry entry = parseLine(line);
                if (entry != null) {
                    entries.add(entry);
                }
            }

            if (rootPath == null) {
                LOGGER.warning("No rootDir in index");
                System.exit(2);
            }

            LOGGER.info("Loaded " + entries.size() + " entries from " + rootPath);
            return new IndexData(rootPath, entries);

        } catch (IOException e) {
            LOGGER.warning("Error: " + e.getMessage());
            System.exit(2);
        }
        return null;
    }

    private static FileEntry parseLine(String line) {
        String[] fields = line.split("\t");
        if (fields.length == 4) {
            String relativePath = fields[0];
            String size = fields[1];
            String mtimeMillis = fields[2];
            String sha256 = fields[3];
            try {
                return new FileEntry(Path.of(relativePath), Long.parseLong(size), Long.parseLong(mtimeMillis), sha256);
            } catch (NumberFormatException e) {
                LOGGER.warning(String.format("Error while parsing line: %s", e.getMessage()));
                System.exit(2);
            }
        }
        return null;
    }
}
