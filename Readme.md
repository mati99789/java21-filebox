# FileBox — File Indexer & Organizer (CLI)

> **Project 1 from the “RAW JAVA 21” series** — pure Java 21, no external libraries, CLI application. The goal is to practice working with the file system, metadata, and change observation.

---

## Table of Contents

* [Description](#description)
* [Key Features](#key-features)
* [Requirements](#requirements)
* [Quick Start](#quick-start)
* [Example Commands](#example-commands)
* [Architecture](#architecture)
* [Index Format (TSV / custom)](#index-format-tsv--custom)
* [Test Data](#test-data)
* [Acceptance Criteria (Definition of Done)](#acceptance-criteria-definition-of-done)
* [Testing & Diagnostics](#testing--diagnostics)
* [Logging](#logging)
* [Metrics & Performance](#metrics--performance)
* [Known Limitations & Design Decisions](#known-limitations--design-decisions)
* [Roadmap & Milestones](#roadmap--milestones)
* [What I Learned](#what-i-learned)
* [Repository Structure](#repository-structure)
* [License](#license)

---

## Description

**FileBox** scans directory trees, builds a **file index** (relative path, size, modification time, optional SHA‑256 hash), detects duplicates, and can **watch** real-time changes. The project focuses on robust error handling, consistent data formats, and ergonomic CLI design.

## Key Features

* `scan <path>` — recursively scan and collect metadata (path, size, mtime).
* `dedupe` — detect duplicates based on SHA‑256 (added in M2).
* `watch <path>` — monitor real-time changes (create/modify/delete).
* `export` — save index to file (TSV or custom format with version header).

## Requirements

* **Java 21 (LTS)**
* **IntelliJ IDEA** or other preferred IDE/editor
* OS: Linux/macOS/Windows (manually tested on at least one)

> **No external libraries.** Pure JDK only.

## Quick Start

1. Clone the repository and open it in IntelliJ as a JDK 21 project.
2. Build and run the application with the "Application (Main)" configuration.
3. Prepare a `data/` directory with sample files for scanning (see **Test Data**).

## Example Commands

```bash
# 1) Full scan from working directory and save index to file
filebox scan ./data --export ./out/index.tsv

# 2) Detect duplicates by SHA-256 (after computing hashes)
filebox dedupe --input ./out/index.tsv --report ./out/dupes.txt

# 3) Watcher: listen for changes and update index on the fly
filebox watch ./data --export ./out/index.tsv
```

> Syntax and options may evolve — use `filebox --help` for the latest info.

## Architecture

Simple layered structure with clear responsibilities:

* **Main** — CLI parsing and orchestration.
* **ScannerService** — recursive traversal, filtering, error handling.
* **FileEntry** — index record: `path` (relative to root), `size` (bytes), `mtime` (epoch millis), `sha256` (optional).
* **IndexStore** — save/load index (TSV / custom format), versioned header.
* **WatcherService** — monitor file events and update index accordingly.

**Operational Assumptions**

* Paths in the index are **relative** to the scan root — portable.
* **Symlinks**: no recursive following (prevent loops).
* Strong error handling: no silent exceptions, always contextual reporting.

## Index Format (TSV / custom)

Minimal record (TSV):

```
version=1
root=./data
path\tsize\tmtime\tsha256
photos/img1.jpg\t204800\t1727352900000\t<optional>
```

* **Header** must include at least `version` and `root`.
* `mtime` in epoch milliseconds.
* `sha256` filled in from **M2** onwards.

## Test Data

Include in `data/` directory:

* Small and large files (≥100 MB) to test buffer handling.
* At least **two identical files** (for `dedupe`).
* Hidden file and directory (e.g. `.hidden/`) plus a **symlink**.
* A file without read permissions (error handling test).

## Acceptance Criteria (Definition of Done)

* `scan` completes without critical exceptions on large trees.
* `export` generates index with **versioned header**.
* `dedupe` outputs consistent hash-based groups.
* `watch` updates index on create/modify/delete.

## Testing & Diagnostics

**No external testing frameworks.**

* **Mini test runner** via CLI, descriptive assertions.
* Scenarios: file count, size sum consistency, index save/load correctness, permission errors, line ending variations (LF/CRLF).
* Cross-check results against system tools (`find`, `du`, etc.).

## Logging

Single-line convention: `timestamp | module | level | message`.

* Default mode: short messages, no stack traces.
* `--verbose` mode: extended diagnostic info.

## Metrics & Performance

* Wall time of scans (small vs large files).
* Hash calculation time (M2) for various sizes.
* (Optional) Record a short session with **Java Flight Recorder**.

## Known Limitations & Design Decisions

* No support for non-standard file streams (local FS only).
* Deduplication purely hash-based (no byte-by-byte re-check).
* Watcher registers new subdirectories on creation; very deep trees may hit OS descriptor limits.

## Roadmap & Milestones

* **M1** — scanner + index export (no hash).
* **M2** — SHA‑256 + `dedupe` + performance measurements.
* **M3** — watcher (directory registration, index updates on events).
* **M4 (optional)** — memory-mapped files and locks (performance, concurrency).

## What I Learned

* Working with `java.nio.file` (walks, attributes, **WatchService**).
* Designing simple file formats with **versioning**.
* Streaming hash calculations and I/O pitfalls.
* CLI design with consistent logging and errors.

## Repository Structure

```
.
├── README.md
├── docs/                # ASCII diagrams, screenshots
├── data/                # input samples (avoid large binaries in repo)
├── out/                 # artifacts (index, duplicate reports)
├── src/main/java/...    # source code
└── src/test/...         # mini test runner (no external libs)
```

## License

MIT — see `LICENSE` file.

---

> **Note**: This application is an educational project for learning Java 21 and good I/O practices. CLI and formats may evolve with releases (`v0.1`, `v0.2`, …) and changelog.
