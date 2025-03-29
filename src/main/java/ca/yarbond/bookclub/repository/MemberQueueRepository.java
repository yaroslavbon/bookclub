package ca.yarbond.bookclub.repository;

import ca.yarbond.bookclub.model.Member;
import ca.yarbond.bookclub.model.MemberQueueItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberQueueRepository extends JpaRepository<MemberQueueItem, Long> {

    List<MemberQueueItem> findByOrderByPositionAsc();

    Optional<MemberQueueItem> findByPosition(int position);

    Optional<MemberQueueItem> findByMember(Member member);

    Optional<MemberQueueItem> findByMemberId(Long memberId);

    @Query("SELECT m FROM MemberQueueItem m WHERE m.active = true ORDER BY m.position ASC")
    List<MemberQueueItem> findByActiveTrue();
}