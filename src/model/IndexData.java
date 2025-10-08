package model;

import java.util.List;

public record IndexData(String path, List<FileEntry> entries) {
}
