package services;

import Model.FileEntry;
import Model.ScanSummary;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;

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
        if(Files.isDirectory(outputFil) || !this.outputPath.contains(".")) {
            outputFil = outputFil.resolve("index.txt");
        }
        Path parnetDir = outputFil.getParent();
        if(parnetDir != null) {
            try {
                Files.createDirectories(parnetDir);
            } catch (IOException e) {
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

            writer.write("# fields=relativePath\tsize\tmtimeMillis");
            writer.newLine();
            writer.newLine();
            for (FileEntry fileEntry : fileEntryList) {
                writer.write(String.format("%s\t%d\t%d", fileEntry.relativePath(), fileEntry.size(), fileEntry.lastModifiedTime()));
                writer.newLine();
            }
            writer.write("======================");
            writer.newLine();
            writer.write(String.format("# files=%d, totalBytes=%d durationMillis=%d ms", scanSummary.totalFiles(), scanSummary.totalBytes(), scanSummary.durationMillis()));
        } catch (IOException e) {
            LOGGER.warning(String.format("Error while writing index file: %s", e.getMessage()));
            System.exit(2);
        }
    }


}
