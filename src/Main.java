import model.FileEntry;
import model.IndexData;
import model.ScanSummary;
import services.IndexStoreService;
import services.ScannerService;
import services.WatcherService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        if (args.length == 0) {
            LOGGER.warning("No arguments provided");
        }

        String command = args.length >= 1 ? args[0] : null;
        String path = args.length > 1 ? args[1] : null;
        String output = args.length > 2 ? args[2] : "index.txt";


        switch (command) {
            case "scan" -> {
                if (path == null) {
                    LOGGER.warning("No path provided");
                    System.exit(1);
                }

                LOGGER.info(String.format("Scanning path: %s", path));

                ScannerService scannerService = new ScannerService(path, output);
                ScanSummary scanSummary = scannerService.scan();

                if (scanSummary != null) {
                    LOGGER.info(String.format("Found %d files, %d MB. Scan took %d ms", scanSummary.totalFiles(), scanSummary.totalBytes()/1000000, scanSummary.durationMillis()));
                } else {
                    LOGGER.warning("Scan failed");
                    System.exit(2);
                }
            }
            case "dedupe" -> {
                String indexPath = args.length > 1 ? args[1] : "index.txt";

                Path indexFile = Path.of(indexPath).toAbsolutePath().normalize();

                if(!Files.exists(indexFile)) {
                    LOGGER.warning("Index file does not exist");
                    System.exit(1);
                }

                IndexData data = IndexStoreService.load(indexFile);

                Map<String, List<FileEntry>> grouped = data.entries().stream().collect(Collectors.groupingBy(FileEntry::sha256));

                Map<String, List<FileEntry>> duplicates = grouped.entrySet().stream().filter(e -> e.getValue().size() > 1).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


                if(duplicates.isEmpty()) {
                    LOGGER.info("No duplicates found");

                    System.exit(0);
                }

                LOGGER.info("Found " + duplicates.size() + " duplicates");

                for(Map.Entry<String, List<FileEntry>> entry : duplicates.entrySet()) {
                    String hash = entry.getKey();
                    List<FileEntry> files = entry.getValue();

                    long size = files.getFirst().size();
                    LOGGER.info(String.format("Group: %d copies, %d bytes, hash: %s",
                            files.size(), size, hash.substring(0, 16) + "..."));

                    for(FileEntry file : files) {
                        LOGGER.info(" - "+ file.relativePath());
                    }
                }
            }
            case "watch" -> {
                if (path == null) {
                    LOGGER.warning("No path provided");
                    System.exit(1);
                }

                Path watchPath = Path.of(path).toAbsolutePath().normalize();
                String indexPath = args.length > 2 ? args[2] : "index.txt";

                IndexData data = IndexStoreService.load(Path.of(indexPath));

                Path indexRootPath = Path.of(data.path()).toAbsolutePath().normalize();

                if(!watchPath.equals(indexRootPath)) {
                    LOGGER.severe("Path mismatch! Watch: " + watchPath + ", Index: " + indexRootPath);
                    System.exit(1);
                }

                try {
                    WatcherService watcher = new WatcherService(indexRootPath, data.entries(), indexPath);
                    watcher.registerDirectory(Path.of(path));

                    Runtime.getRuntime().addShutdownHook(new Thread(() ->{
                        LOGGER.info("Shutting down watcher");
                        watcher.stop();
                    }));

                    watcher.start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            case "help", "-h", "--help" -> {
                printUsage();
                System.exit(0);
            }
            case null, default -> {
                printUsage();
                System.exit(1);
            }
        }


    }

    private static void printUsage() {
        System.out.println("""
                Usage: filebox [command] [path] [output]
                Commands: scan
                Output: path to output file (default: index.txt)
                Help: filebox -h or --help or filebox help
                """);
    }
}