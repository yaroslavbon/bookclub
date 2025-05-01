package ca.yarbond.bookclub.service;

import ca.yarbond.bookclub.model.Book;
import ca.yarbond.bookclub.model.Member;
import ca.yarbond.bookclub.model.Rating;
import ca.yarbond.bookclub.repository.BookRepository;
import ca.yarbond.bookclub.repository.MemberRepository;
import ca.yarbond.bookclub.repository.RatingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;

    @Autowired
    public RatingService(
            RatingRepository ratingRepository,
            BookRepository bookRepository,
            MemberRepository memberRepository) {
        this.ratingRepository = ratingRepository;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
    }

    public List<Rating> getRatingsByBookId(Long bookId) {
        return ratingRepository.findByBookId(bookId);
    }

    public List<Rating> getRatingsByMemberId(Long memberId) {
        return ratingRepository.findByMemberId(memberId);
    }

    public Rating getRatingById(Long id) {
        return ratingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rating not found with id: " + id));
    }

    /**
     * Creates or updates a rating
     */
    @Transactional
    public Rating createOrUpdateRating(Long bookId, Long memberId, Integer readabilityRating,
                                       Integer contentRating, String comments) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + bookId));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found with id: " + memberId));

        // Check if rating already exists
        Optional<Rating> existingRating = ratingRepository.findByBookIdAndMemberId(bookId, memberId);

        if (existingRating.isPresent()) {
            Rating rating = existingRating.get();
            rating.setReadabilityRating(readabilityRating);
            rating.setContentRating(contentRating);
            rating.setComments(comments);
            return ratingRepository.save(rating);
        } else {
            Rating rating = new Rating();
            rating.setBook(book);
            rating.setMember(member);
            rating.setReadabilityRating(readabilityRating);
            rating.setContentRating(contentRating);
            rating.setComments(comments);
            return ratingRepository.save(rating);
        }
    }

    // Removed markAsDidNotRead method as it's no longer needed

    public void deleteRating(Long id) {
        ratingRepository.deleteById(id);
    }

    /**
     * Gets average ratings for a book
     * Returns a map with keys "readability" and "content"
     */
    public Map<String, Double> getAverageRatings(Long bookId) {
        List<Rating> ratings = ratingRepository.findByBookId(bookId);

        if (ratings.isEmpty()) {
            return Map.of("readability", 0.0, "content", 0.0);
        }

        double avgReadability = ratings.stream()
                .mapToInt(r -> r.getReadabilityRating() != null ? r.getReadabilityRating() : 0)
                .average()
                .orElse(0);

        double avgContent = ratings.stream()
                .mapToInt(r -> r.getContentRating() != null ? r.getContentRating() : 0)
                .average()
                .orElse(0);

        return Map.of("readability", avgReadability, "content", avgContent);
    }

    /**
     * Gets average ratings for multiple books
     * Returns a map of bookId -> {readability, content}
     */
    public Map<Long, Map<String, Double>> getAverageRatingsForBooks(List<Long> bookIds) {
        return bookIds.stream()
                .collect(Collectors.toMap(
                        bookId -> bookId,
                        this::getAverageRatings
                ));
    }
}