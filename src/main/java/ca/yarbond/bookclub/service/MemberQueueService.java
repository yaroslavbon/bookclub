package ca.yarbond.bookclub.service;

import ca.yarbond.bookclub.model.Member;
import ca.yarbond.bookclub.model.MemberQueueItem;
import ca.yarbond.bookclub.repository.MemberQueueRepository;
import ca.yarbond.bookclub.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MemberQueueService {

    private final MemberQueueRepository memberQueueRepository;
    private final MemberRepository memberRepository;

    @Autowired
    public MemberQueueService(
            MemberQueueRepository memberQueueRepository,
            MemberRepository memberRepository) {
        this.memberQueueRepository = memberQueueRepository;
        this.memberRepository = memberRepository;
    }

    /**
     * Gets the ordered list of members in the queue
     */
    public List<MemberQueueItem> getQueue() {
        return memberQueueRepository.findByOrderByPositionAsc();
    }


    /**
     * Gets the current member (position 0)
     */
    public MemberQueueItem getCurrentMember() {
        return memberQueueRepository.findByPosition(0)
                .orElseThrow(() -> new RuntimeException("No member at position 0 found"));
    }

    /**
     * Gets the next member in queue (position 1)
     */
    public MemberQueueItem getNextMember() {
        return memberQueueRepository.findByPosition(1)
                .orElseThrow(() -> new RuntimeException("No member at position 1 found"));
    }

    /**
     * Gets a queue item by member ID
     */
    public MemberQueueItem getQueueItemByMemberId(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found with id: " + memberId));

        return memberQueueRepository.findByMember(member)
                .orElseThrow(() -> new RuntimeException("Member not found in queue: " + member.getName()));
    }

    /**
     * Adds a new member to the end of the queue
     */
    @Transactional
    public void addMemberToQueue(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found with id: " + memberId));

        // Check if member already exists in queue
        Optional<MemberQueueItem> existingQueueItem = memberQueueRepository.findByMember(member);
        if (existingQueueItem.isPresent()) {
            return;
        }

        // Find the highest position
        int maxPosition = memberQueueRepository.findAll().stream()
                .mapToInt(MemberQueueItem::getPosition)
                .max()
                .orElse(-1);

        // Create new queue item at the end of the queue
        MemberQueueItem newQueueItem = new MemberQueueItem();
        newQueueItem.setMember(member);
        newQueueItem.setPosition(maxPosition + 1);

        memberQueueRepository.save(newQueueItem);
    }

    /**
     * Moves a member to a specific position in the queue
     */
    @Transactional
    public void moveMemberToPosition(Long memberId, int newPosition) {
        MemberQueueItem queueItem = getQueueItemByMemberId(memberId);
        int oldPosition = queueItem.getPosition();

        // No change needed
        if (oldPosition == newPosition) {
            return;
        }

        // Adjust positions for all affected members
        List<MemberQueueItem> allMembers = getQueue();

        if (oldPosition < newPosition) {
            // Moving down - shift members between oldPosition+1 and newPosition UP by one position
            for (MemberQueueItem member : allMembers) {
                int pos = member.getPosition();
                // Only affect members BETWEEN the old and new positions, excluding the moved member
                if (pos > oldPosition && pos <= newPosition && !member.getMember().getId().equals(memberId)) {
                    member.setPosition(pos - 1);
                }
            }
        } else { // oldPosition > newPosition
            // Moving up - shift members between newPosition and oldPosition-1 DOWN by one position
            for (MemberQueueItem member : allMembers) {
                int pos = member.getPosition();
                // Only affect members BETWEEN the new and old positions, excluding the moved member
                if (pos >= newPosition && pos < oldPosition && !member.getMember().getId().equals(memberId)) {
                    member.setPosition(pos + 1);
                }
            }
        }

        // Set new position for the moved member
        queueItem.setPosition(newPosition);
        memberQueueRepository.saveAll(allMembers);
    }

    /**
     * Rotates the queue - current member moves to the end, everyone else moves up
     */
    @Transactional
    public void rotateQueue() {
        rotateQueueByPositions(1);
    }

    /**
     * Rotates the queue by a specified number of positions.
     * Positive values move members forward in the queue (current members to the end),
     * while negative values move members backward (members from the end to the front).
     *
     * @param positions Number of positions to rotate. Must be less than the queue size.
     */
    @Transactional
    public void rotateQueueByPositions(int positions) {
        List<MemberQueueItem> allMembers = getQueue();
        int queueSize = allMembers.size();

        // Nothing to rotate if queue has 0 or 1 members
        if (queueSize <= 1) {
            return;
        }

        // Normalize positions to be positive (for forward rotation)
        int effectivePositions = positions % queueSize;
        if (effectivePositions < 0) {
            effectivePositions += queueSize;
        }

        // Skip if no effective rotation
        if (effectivePositions == 0) {
            return;
        }

        // Calculate and set new positions
        for (MemberQueueItem item : allMembers) {
            int oldPosition = item.getPosition();
            int newPosition = (oldPosition + queueSize - effectivePositions) % queueSize;
            item.setPosition(newPosition);

        }
        memberQueueRepository.saveAll(allMembers);
    }

    @Transactional
    public void rotateToMember(Member member) {
        MemberQueueItem queueItemByMemberId = getQueueItemByMemberId(member.getId());
        int numOfPositionsToRotate = queueItemByMemberId.getPosition();

        if(numOfPositionsToRotate == 0) {
            return;
        }

        rotateQueueByPositions(numOfPositionsToRotate);
    }

    /**
     * Skips the current member, placing them after the specified member
     */
    @Transactional
    public void skipCurrentMember(Long placeAfterId) {
        MemberQueueItem currentMember = getCurrentMember();
        MemberQueueItem placeAfterMember = getQueueItemByMemberId(placeAfterId);

        int newPosition = placeAfterMember.getPosition();
        moveMemberToPosition(currentMember.getMember().getId(), newPosition);
    }

    /**
     * Removes a member from the queue
     */
    @Transactional
    public void removeMemberFromQueue(Long memberId) {
        MemberQueueItem queueItem = getQueueItemByMemberId(memberId);
        int position = queueItem.getPosition();

        // Delete the queue item
        memberQueueRepository.delete(queueItem);

        // Adjust positions for all members after the removed one
        List<MemberQueueItem> allMembers = getQueue();
        allMembers.forEach(m -> {
            if (m.getPosition() > position) {
                m.setPosition(m.getPosition() - 1);
                memberQueueRepository.save(m);
            }
        });
    }
}