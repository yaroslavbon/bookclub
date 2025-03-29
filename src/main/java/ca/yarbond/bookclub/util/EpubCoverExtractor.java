package ca.yarbond.bookclub.util;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class EpubCoverExtractor {

    private final String coverStoragePath;

    public EpubCoverExtractor(@Value("${app.storage.covers-location}") String coverStoragePath) {
        this.coverStoragePath = coverStoragePath;
    }

    /**
     * Extract cover image from EPUB file and save it to the storage directory
     *
     * @param epubFile The uploaded EPUB file
     * @return Path to the extracted cover image, or null if extraction failed
     */
    public String extractCover(MultipartFile epubFile) {
        try {
            EpubReader epubReader = new EpubReader();

            try (InputStream inputStream = epubFile.getInputStream()) {
                Book book = epubReader.readEpub(inputStream);

                // Check if book has a cover
                if (book.getCoverImage() == null) {
                    return null;
                }

                // Convert cover data to image
                byte[] coverData = book.getCoverImage().getData();
                BufferedImage coverImage = ImageIO.read(new ByteArrayInputStream(coverData));

                // Generate unique filename
                String filename = UUID.randomUUID() + ".jpg";
                Path coverPath = Paths.get(coverStoragePath, filename);
                File outputFile = coverPath.toFile();

                // Ensure directory exists
                outputFile.getParentFile().mkdirs();

                // Save image to file
                ImageIO.write(coverImage, "jpg", outputFile);

                return filename;
            }
        } catch (IOException e) {
            System.err.println("Failed to extract cover image: " + e.getMessage());
            return null;
        }
    }
}