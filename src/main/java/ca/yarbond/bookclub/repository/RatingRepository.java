package ca.yarbond.bookclub.repository;

import ca.yarbond.bookclub.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findByBookId(Long bookId);

    List<Rating> findByMemberId(Long memberId);

    Optional<Rating> findByBookIdAndMemberId(Long bookId, Long memberId);
}