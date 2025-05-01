package ca.yarbond.bookclub.service;

import ca.yarbond.bookclub.model.Member;
import ca.yarbond.bookclub.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberQueueService memberQueueService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public MemberService(MemberRepository memberRepository, MemberQueueService memberQueueService, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.memberQueueService = memberQueueService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Get all members
     */
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }
    
    /**
     * Get all active members
     */
    public List<Member> getActiveMembers() {
        return memberRepository.findAll().stream()
                .filter(Member::isActive)
                .toList();
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
     * Note: Active members are automatically added to queue by the controller
     */
    @Transactional
    public Member createMember(Member member, String password) {
        if (memberRepository.existsByName(member.getName())) {
            throw new RuntimeException("Member with name '" + member.getName() + "' already exists");
        }

        // Default to active and user role
        member.setActive(true);
        member.setRole(Member.Role.USER);
        member.setPasswordHash(passwordEncoder.encode(password));
        
        Member createdMember = memberRepository.save(member);
        memberQueueService.addMemberToQueue(createdMember.getId());
        return createdMember;
    }

    /**
     * Update an existing member
     * Note: The controller handles synchronizing active status with queue membership
     */
    @Transactional
    public Member updateMemberName(Long id, String newName) {
        Member member = getMemberById(id);

        // Check if name is being changed and if new name already exists
        if (!member.getName().equals(newName) &&
                memberRepository.existsByName(newName)) {
            throw new RuntimeException("Member with name '" + newName + "' already exists");
        }

        member.setName(newName);

        return memberRepository.save(member);
    }
    
    /**
     * Reset member password
     */
    @Transactional
    public String resetPassword(Long id) {
        Member member = getMemberById(id);
        
        // Generate a random password
        String newPassword = generateRandomPassword();
        
        // Update password hash
        member.setPasswordHash(passwordEncoder.encode(newPassword));
        memberRepository.save(member);
        
        return newPassword;
    }
    
    /**
     * Generate a cryptographically strong random password
     */
    public String generateRandomPassword() {
        // Create a random password of length 10
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+";
        StringBuilder sb = new StringBuilder();
        java.security.SecureRandom random = new java.security.SecureRandom();
        
        for (int i = 0; i < 10; i++) {
            int randomIndex = random.nextInt(chars.length());
            sb.append(chars.charAt(randomIndex));
        }
        
        return sb.toString();
    }


    @Transactional
    public Member toggleStatus(Long id) {
        Member member = getMemberById(id);

        boolean nowActive = !member.isActive();

        member.setActive(nowActive);

        Member updatedMember = memberRepository.save(member);

        if (nowActive)
            memberQueueService.addMemberToQueue(id);
        else
            memberQueueService.removeMemberFromQueue(id);

        return updatedMember;
    }

    /**
     * Delete a member
     *
     * @return
     */
    @Transactional
    public String deleteMember(Long id) {
        Member member = getMemberById(id);
        String memberName = member.getName();

        // Remove from queue if member is in queue
        if(member.isActive()) memberQueueService.removeMemberFromQueue(id);

        memberRepository.deleteById(id);

        return memberName;
    }

}