package ca.yarbond.bookclub.service;

import ca.yarbond.bookclub.model.Member;
import ca.yarbond.bookclub.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    @Autowired
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    /**
     * Get all members
     */
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    /**
     * Get member by ID
     */
    public Member getMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found with id: " + id));
    }

    /**
     * Get member by name
     */
    public Member getMemberByName(String name) {
        return memberRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Member not found with name: " + name));
    }

    /**
     * Check if member exists by name
     */
    public boolean memberExistsByName(String name) {
        return memberRepository.existsByName(name);
    }

    /**
     * Create a new member
     */
    @Transactional
    public Member createMember(Member member) {
        if (memberRepository.existsByName(member.getName())) {
            throw new RuntimeException("Member with name '" + member.getName() + "' already exists");
        }
        return memberRepository.save(member);
    }

    /**
     * Update an existing member
     */
    @Transactional
    public Member updateMember(Long id, Member memberDetails) {
        Member member = getMemberById(id);

        // Check if name is being changed and if new name already exists
        if (!member.getName().equals(memberDetails.getName()) &&
                memberRepository.existsByName(memberDetails.getName())) {
            throw new RuntimeException("Member with name '" + memberDetails.getName() + "' already exists");
        }

        member.setName(memberDetails.getName());

        return memberRepository.save(member);
    }

    /**
     * Delete a member
     */
    @Transactional
    public void deleteMember(Long id) {
        // Before deleting, should check if member has books or is in queue
        if (!memberRepository.existsById(id)) {
            throw new RuntimeException("Member not found with id: " + id);
        }
        memberRepository.deleteById(id);
    }
}