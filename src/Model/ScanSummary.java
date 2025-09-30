package Model;

public record ScanSummary(String rootPath, long totalBytes, long totalFiles, long durationMillis) {
}
