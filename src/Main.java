import Model.ScanSummary;
import services.ScannerService;

import java.util.logging.Logger;

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
                ScanSummary scanSummary = new ScannerService(path, output).scan();

                if (scanSummary != null) {
                    LOGGER.info(String.format("Found %d files, %d bytes. Scan took %d ms", scanSummary.totalFiles(), scanSummary.totalBytes(), scanSummary.durationMillis()));
                } else {
                    LOGGER.warning("Scan failed");
                    System.exit(2);
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