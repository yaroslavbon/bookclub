package ca.yarbond.bookclub.config;

import ca.yarbond.bookclub.model.Member;
import ca.yarbond.bookclub.repository.MemberRepository;
import ca.yarbond.bookclub.service.MemberQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberQueueService memberQueueService;

    // Default values that will be overridden by command-line arguments
    private String adminUsername = "admin";
    private String adminPassword = "admin";

    @Autowired
    public UserInitializer(MemberRepository memberRepository, 
                          PasswordEncoder passwordEncoder,
                          MemberQueueService memberQueueService) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.memberQueueService = memberQueueService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (args.containsOption("set-admin") || memberRepository.count() == 0) {
            // Get username and password from arguments if provided
            if (args.containsOption("admin-username")) {
                adminUsername = args.getOptionValues("admin-username").get(0);
            }
            
            if (args.containsOption("admin-password")) {
                adminPassword = args.getOptionValues("admin-password").get(0);
            }
            
            // Create or update admin user
            Member adminMember = memberRepository.findByName(adminUsername).orElseGet(Member::new);
            adminMember.setName(adminUsername);
            adminMember.setPasswordHash(passwordEncoder.encode(adminPassword));
            adminMember.setRole(Member.Role.ADMIN);
            adminMember.setActive(true);
            Member savedMember = memberRepository.save(adminMember);
            
            // Add admin to the queue
            memberQueueService.addMemberToQueue(savedMember.getId());
            
            System.out.println("Admin user '" + adminUsername + "' has been created/updated");
        }
    }
}