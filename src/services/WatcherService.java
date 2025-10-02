package services;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static java.nio.file.StandardWatchEventKinds.*;

public class WatcherService {

    private static Logger LOGGER = Logger.getLogger(WatcherService.class.getName());
    private final java.nio.file.WatchService INSTANCE;
    private final Map<WatchKey, Path> KEYS = new ConcurrentHashMap<>();
    private boolean running = false;

    public WatcherService() throws IOException {
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
                    if (Files.isDirectory(fullPath)) {
                        registerTree(fullPath);
                    }
                }

                if(kind == ENTRY_MODIFY) {
                    if (Files.isRegularFile(fullPath)) {
                        LOGGER.info("File modified " + fullPath);
                    }
                }

                if(kind == ENTRY_DELETE) {
                    LOGGER.info("File deleted " + fullPath);
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

    public void handleEvent(WatchEvent event, Path path) {
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

}
