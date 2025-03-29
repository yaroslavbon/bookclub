package ca.yarbond.bookclub.service;

import ca.yarbond.bookclub.model.Book;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path booksStorageLocation;
    private final Path coversStorageLocation;
    private final long maxFileSize;
    private final EpubCoverExtractor epubCoverExtractor;

    // Set of allowed file extensions
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(
            Arrays.asList("epub", "pdf", "mobi"));

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

    public String storeBookFile(MultipartFile file, Book book) {
        try {
            // Check if file is empty
            if (file.isEmpty()) {
                throw new RuntimeException("Cannot store empty file");
            }

            // Check file size
            if (file.getSize() > maxFileSize) {
                throw new RuntimeException("File size exceeds maximum limit of " + (maxFileSize / 1024 / 1024) + "MB");
            }

            // Get file extension from original filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);

            // Validate file extension
            if (!ALLOWED_EXTENSIONS.contains(fileExtension.toLowerCase())) {
                throw new RuntimeException("Invalid file format. Allowed formats are: epub, pdf, mobi");
            }

            // Create readable filename based on book title and author
            // Both title and author can contain Cyrillic characters
            String safeTitle = sanitizeFileName(book.getTitle());
            String safeAuthor = sanitizeFileName(book.getAuthor());

            // Combine title and author into a nice filename
            String filename = safeTitle;
            if (safeAuthor != null && !safeAuthor.isEmpty() && !safeAuthor.equals("untitled")) {
                filename = filename + "-" + safeAuthor;
            }

             String randomSuffix = UUID.randomUUID().toString().substring(0, 4);
            filename = filename + "-" + randomSuffix;


            // Add the extension
            filename = filename + "." + fileExtension.toLowerCase();

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

    public void deleteBookFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            return;
        }

        try {
            Path filePath = this.booksStorageLocation.resolve(filename).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Could not delete file: " + filename, ex);
        }
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

    public String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex < 0) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * Sanitize filename by removing problematic characters for filesystem
     * while preserving Cyrillic and other non-Latin characters
     */
    private String sanitizeFileName(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "untitled";
        }

        // Remove characters that are invalid in filenames across most filesystems
        // (/, \, :, *, ?, ", <, >, |) but preserve Cyrillic and other Unicode characters
        String sanitized = input.trim()
                .replaceAll("[\\\\/:*?\"<>|]", "") // Remove problematic filesystem characters
                .replaceAll("\\s+", "-");          // Replace spaces with dashes

        // If after sanitizing we have an empty string (rare, but possible if input was only invalid chars)
        if (sanitized.trim().isEmpty()) {
            return "untitled";
        }

        // Truncate to reasonable length if needed (max 100 chars to allow for file extension)
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }

        return sanitized;
    }
}