package ca.yarbond.bookclub.service;

import ca.yarbond.bookclub.util.EpubCoverExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path booksStorageLocation;
    private final Path coversStorageLocation;
    private final long maxFileSize;
    private final EpubCoverExtractor epubCoverExtractor;

    @Autowired
    public FileStorageService(
            @Value("${app.storage.books-location}") String booksStorageLocation,
            @Value("${app.storage.covers-location}") String coversStorageLocation,
            @Value("${app.storage.max-file-size}") long maxFileSize,
            EpubCoverExtractor epubCoverExtractor) {

        this.booksStorageLocation = Paths.get(booksStorageLocation).toAbsolutePath().normalize();
        this.coversStorageLocation = Paths.get(coversStorageLocation).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize;
        this.epubCoverExtractor = epubCoverExtractor;

        try {
            Files.createDirectories(this.booksStorageLocation);
            Files.createDirectories(this.coversStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create the storage directories", ex);
        }
    }

    public String storeBookFile(MultipartFile file) {
        try {
            // Check if file is empty
            if (file.isEmpty()) {
                throw new RuntimeException("Cannot store empty file");
            }

            // Check file size
            if (file.getSize() > maxFileSize) {
                throw new RuntimeException("File size exceeds maximum limit of " + (maxFileSize / 1024 / 1024) + "MB");
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String filename = UUID.randomUUID() + "." + fileExtension;

            // Save the file
            Path targetLocation = this.booksStorageLocation.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return filename;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file", ex);
        }
    }

    public String extractCoverFromEpub(MultipartFile file) {
        if (file.isEmpty() || !getFileExtension(file.getOriginalFilename()).equalsIgnoreCase("epub")) {
            return null;
        }

        return epubCoverExtractor.extractCover(file);
    }

    public Path loadBookAsResource(String filename) {
        try {
            Path filePath = this.booksStorageLocation.resolve(filename).normalize();
            if (!Files.exists(filePath)) {
                throw new RuntimeException("File not found: " + filename);
            }
            return filePath;
        } catch (Exception ex) {
            throw new RuntimeException("Could not load file: " + filename, ex);
        }
    }

    public Path loadCoverAsResource(String filename) {
        try {
            Path filePath = this.coversStorageLocation.resolve(filename).normalize();
            if (!Files.exists(filePath)) {
                throw new RuntimeException("Cover image not found: " + filename);
            }
            return filePath;
        } catch (Exception ex) {
            throw new RuntimeException("Could not load cover image: " + filename, ex);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex < 0) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}