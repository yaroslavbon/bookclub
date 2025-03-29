package ca.yarbond.bookclub.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class BackupService {

    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm");

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${app.storage.books-location}")
    private String booksStorageLocation;

    @Value("${app.storage.covers-location}")
    private String coversStorageLocation;

    @Value("${app.backup.location:./backups}")
    private String backupLocation;

    @Value("${app.backup.enabled:false}")
    private boolean backupEnabled;

    /**
     * Scheduled backup that runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledBackup() {
        if (!backupEnabled) {
            logger.info("Scheduled backup is disabled");
            return;
        }

        try {
            performBackup();
            logger.info("Scheduled backup completed successfully");
        } catch (Exception e) {
            logger.error("Scheduled backup failed", e);
        }
    }

    /**
     * Performs a manual backup
     * @return Path to the backup file
     */
    public String performBackup() throws IOException {
        // Create backup directory if it doesn't exist
        Path backupDir = Paths.get(backupLocation);
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
        }

        // Create timestamped backup file
        String timestamp = dateFormat.format(new Date());
        String backupFileName = "bookclub-backup-" + timestamp + ".zip";
        Path backupFile = backupDir.resolve(backupFileName);

        // Create zip file
        try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(backupFile))) {
            // Backup database file
            backupDatabase(zipOut);

            // Backup book files
            backupDirectory(zipOut, Paths.get(booksStorageLocation), "books");

            // Backup cover images
            backupDirectory(zipOut, Paths.get(coversStorageLocation), "covers");
        }

        return backupFile.toString();
    }

    private void backupDatabase(ZipOutputStream zipOut) throws IOException {
        // Extract database file path from JDBC URL
        String dbFilePath = extractDbFilePath(dbUrl);
        if (dbFilePath != null) {
            Path dbFile = Paths.get(dbFilePath);
            if (Files.exists(dbFile)) {
                addFileToZip(zipOut, dbFile, "database/" + dbFile.getFileName());
            }
        }
    }

    private String extractDbFilePath(String jdbcUrl) {
        // Example: jdbc:h2:file:./bookclub_db;DB_CLOSE_ON_EXIT=FALSE
        if (jdbcUrl.startsWith("jdbc:h2:file:")) {
            String path = jdbcUrl.substring("jdbc:h2:file:".length());
            // Remove any parameters
            if (path.contains(";")) {
                path = path.substring(0, path.indexOf(';'));
            }
            return path + ".mv.db"; // H2 file extension
        }
        return null;
    }

    private void backupDirectory(ZipOutputStream zipOut, Path directory, String zipDirName) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        Files.list(directory).forEach(file -> {
            try {
                if (Files.isRegularFile(file)) {
                    addFileToZip(zipOut, file, zipDirName + "/" + file.getFileName());
                }
            } catch (IOException e) {
                logger.error("Failed to backup file: " + file, e);
            }
        });
    }

    private void addFileToZip(ZipOutputStream zipOut, Path file, String entryName) throws IOException {
        ZipEntry zipEntry = new ZipEntry(entryName);
        zipOut.putNextEntry(zipEntry);
        Files.copy(file, zipOut);
        zipOut.closeEntry();
    }
}