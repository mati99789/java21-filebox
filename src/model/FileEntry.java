package model;

import java.nio.file.Path;

public record FileEntry(Path relativePath, long size, long lastModifiedTime, String sha256) {
}
