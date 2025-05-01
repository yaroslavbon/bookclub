package ca.yarbond.bookclub.config;

import ca.yarbond.bookclub.model.Member;
import ca.yarbond.bookclub.repository.MemberRepository;
import ca.yarbond.bookclub.service.MemberQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("prod") // Only active in production environment
public class UserInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberQueueService memberQueueService;

    @Value("${app.security.admin.username:admin}")
    private String adminUsername;

    @Value("${app.security.admin.password:admin}")
    private String adminPassword;

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
        if (args.containsOption("set-admin")) {
            Member adminMember = memberRepository.findByName(adminUsername).orElseGet(Member::new);
            adminMember.setName(adminUsername);
            adminMember.setPasswordHash(passwordEncoder.encode(adminPassword));
            adminMember.setRole(Member.Role.ADMIN);
            adminMember.setActive(true);
            Member savedMember = memberRepository.save(adminMember);
            
            // Add admin to the queue
            memberQueueService.addMemberToQueue(savedMember.getId());
        }
    }
}