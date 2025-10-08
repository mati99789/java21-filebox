package services;

import model.FileEntry;
import model.ScanSummary;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static java.nio.file.StandardWatchEventKinds.*;

public class WatcherService {
    private static Logger LOGGER = Logger.getLogger(WatcherService.class.getName());
    private final java.nio.file.WatchService INSTANCE;
    private final Map<WatchKey, Path> KEYS = new ConcurrentHashMap<>();
    private boolean running = false;

    private final Path rootPath;
    private final Map<Path, FileEntry> index;
    private final String indexFilePath;

    public WatcherService(Path rootPath, List<FileEntry> entries, String indexFilePath) throws IOException {
        this.rootPath = rootPath;
        this.indexFilePath = indexFilePath;

        this.index = new ConcurrentHashMap<>();
        for(FileEntry entry : entries) {
            index.put(entry.relativePath(), entry);

        }
        this.INSTANCE = FileSystems.getDefault().newWatchService();
    }

    public void start() throws IOException {
        if (KEYS.isEmpty()) {
            LOGGER.info("Register at least one directory");
            System.exit(0);
        }

        this.running = true;
        LOGGER.info("Starting watch service  " + this.KEYS.size() + " directories (recursive). Press ctrl+c to stop");

        while (running) {
            WatchKey key;
            try {
                key = this.INSTANCE.take();
            } catch (InterruptedException e) {
                LOGGER.warning(e.getMessage());
                break;
            }

            Path baseDir = KEYS.get(key);
            if (baseDir == null) {
                LOGGER.warning("Unknown WatchKey (no baseDir).");
                key.reset();
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                Path relativePath = (Path) event.context();
                Path fullPath = baseDir.resolve(relativePath);


                LOGGER.info(String.format("[%s] %s",kind.name(), fullPath));
                if (kind == OVERFLOW) {
                    LOGGER.warning("Overflow");
                    continue;
                }

                if (kind == ENTRY_CREATE) {
                    if(Files.isRegularFile(fullPath)){

                    Path relativeFromRoot = rootPath.relativize(fullPath);
                    FileEntry entry = createFileEntry(fullPath, relativeFromRoot);
                    index.put(relativeFromRoot, entry);
                    saveIndex();
                    } else if (Files.isDirectory(fullPath)) {
                        registerTree(fullPath);
                    }
                }

                if (kind == ENTRY_MODIFY && Files.isRegularFile(fullPath)) {
                    Path relativeFromRoot = rootPath.relativize(fullPath);
                    FileEntry entry = createFileEntry(fullPath, relativeFromRoot);
                    index.put(relativeFromRoot, entry);
                    saveIndex();
                }

                if (kind == ENTRY_DELETE) {
                    Path relativeFromRoot = rootPath.relativize(fullPath);
                    index.remove(relativeFromRoot);
                    saveIndex();
                }

            }

            boolean valid = key.reset();
            if (!valid) {
                this.KEYS.remove(key);

                if (this.KEYS.isEmpty()) {
                    LOGGER.info("No directories found to watch. Stopping");
                    this.stop();
                }
            }
        }
    }

    public void stop() {
        try {
            this.running = false;
            this.INSTANCE.close();
        } catch (IOException e) {
            LOGGER.warning(e.getMessage());
            System.exit(2);
        }

    }

    public void registerOne(Path path) {
        try {
            WatchKey key = path.register(this.INSTANCE, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            this.KEYS.put(key, path);
        } catch (IOException e) {
            LOGGER.warning(e.getMessage());
            System.exit(2);
        }
    }

    public void registerTree(Path path) {
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    registerOne(dir);
                    return super.preVisitDirectory(dir, attrs);
                }
            });
        } catch (IOException e) {
            LOGGER.warning(e.getMessage());
            System.exit(2);
        }
    }

    public void registerDirectory(Path path) {
        if (!Files.isDirectory(path)) {
            LOGGER.warning(path + " is not a directory");
            System.exit(2);
        }
        registerTree(path.toAbsolutePath().normalize());
    }

    private FileEntry createFileEntry(Path absolutePath, Path relativePath) {
        try {
            long size = Files.size(absolutePath);
            long mtime = Files.getLastModifiedTime(absolutePath).toMillis();
            String sha256 = calculateSha256(absolutePath);
            return new FileEntry(relativePath, size, mtime, sha256);
        } catch (IOException e) {
            LOGGER.warning("Cannot read file: " + absolutePath + " - " + e.getMessage());
            return null;
        }

    }

    private String calculateSha256(Path absolutePath) {
        try(BufferedInputStream reader = new BufferedInputStream(Files.newInputStream(absolutePath))) {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = reader.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, bytesRead);
            }

            return HexFormat.of().formatHex(messageDigest.digest());

        }catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveIndex() {
        List<FileEntry> entries = new ArrayList<>(index.values());

        long totalBytes = entries.stream().mapToLong(FileEntry::size).sum();

        ScanSummary scanSummary = new ScanSummary(rootPath.toString(), totalBytes, entries.size(), 0);

        new IndexStoreService(scanSummary, indexFilePath).save(entries);

        LOGGER.info("Index saved: " + entries.size() + " files");
    }
}
