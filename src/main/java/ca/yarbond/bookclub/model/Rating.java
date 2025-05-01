package ca.yarbond.bookclub.model;

import lombok.Data;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Table(name = "ratings")
@Data
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Book book;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Member member;

    @Column(name = "readability_rating")
    private Integer readabilityRating; // 1-5 scale

    @Column(name = "content_rating")
    private Integer contentRating; // 1-5 scale

    @Column(length = 1000)
    private String comments;

    @Column(name = "date_rated")
    private LocalDate dateRated = LocalDate.now();
}