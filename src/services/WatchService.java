package services;

import java.io.IOException;
import java.nio.file.*;

public class WatchService {

    private final java.nio.file.WatchService INSTANCE;

    public WatchService() throws IOException {
        this.INSTANCE = FileSystems.getDefault().newWatchService();
    }

    public void start() throws IOException {
    }

    public void stop() {
    }

    public void handleEvent(WatchEvent event, Path path) {
    }

    public void registerDirectory(Path path) {
    }
}
