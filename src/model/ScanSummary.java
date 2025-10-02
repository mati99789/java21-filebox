package model;

public record ScanSummary(String rootPath, long totalBytes, long totalFiles, long durationMillis) {
}
