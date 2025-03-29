package ca.yarbond.bookclub.controller;

import ca.yarbond.bookclub.service.BookService;
import ca.yarbond.bookclub.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Path;

@Controller
@RequestMapping("/files")
public class FileController {

    private final FileStorageService fileStorageService;
    private final BookService bookService;

    @Autowired
    public FileController(FileStorageService fileStorageService, BookService bookService) {
        this.fileStorageService = fileStorageService;
        this.bookService = bookService;
    }

    @GetMapping("/books/{filename:.+}")
    public ResponseEntity<Resource> downloadBook(@PathVariable String filename) {
        try {
            Path filePath = fileStorageService.loadBookAsResource(filename);
            Resource resource = new UrlResource(filePath.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/covers/{filename:.+}")
    public ResponseEntity<Resource> downloadCover(@PathVariable String filename) {
        try {
            Path filePath = fileStorageService.loadCoverAsResource(filename);
            Resource resource = new UrlResource(filePath.toUri());

            // Determine media type based on file extension
            String contentType = "image/jpeg";
            if (filename.toLowerCase().endsWith(".png")) {
                contentType = "image/png";
            } else if (filename.toLowerCase().endsWith(".gif")) {
                contentType = "image/gif";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a book file
     */
    @PostMapping("/delete-book-file")
    public String deleteBookFile(
            @RequestParam Long bookId,
            @RequestParam String format,
            RedirectAttributes redirectAttributes) {
        try {
            bookService.removeBookFile(bookId, format);
            redirectAttributes.addFlashAttribute("successMessage",
                    format.toUpperCase() + " file removed successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to remove file: " + e.getMessage());
        }
        return "redirect:/books/" + bookId;
    }
}