package ca.yarbond.bookclub.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "books")
@Data
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(name = "is_fiction")
    private boolean fiction;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Member owner;

    @Enumerated(EnumType.STRING)
    private BookStatus status;

    @Column(name = "comments", length = 1000)
    private String comments;

    @Column(name = "cover_image_path")
    private String coverImagePath;

    @Column(name = "completion_date")
    private LocalDate completionDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "file_paths", columnDefinition = "json")
    private Map<String, String> filePaths = new HashMap<>();

    // Helper methods to manage file paths
    public void addFilePath(String format, String path) {
        if (filePaths == null) {
            filePaths = new HashMap<>();
        }
        filePaths.put(format.toLowerCase(), path);
    }

    public String getFilePath(String format) {
        if (filePaths == null) {
            return null;
        }
        return filePaths.get(format.toLowerCase());
    }

    /**
     * Remove a file path for the given format
     *
     * @param format The file format (epub, pdf, mobi)
     */
    public void removeFilePath(String format) {
        if (this.filePaths != null) {
            this.filePaths.remove(format);
        }
    }
}