package ca.yarbond.bookclub.config;

import ca.yarbond.bookclub.model.Book;
import ca.yarbond.bookclub.model.BookStatus;
import ca.yarbond.bookclub.model.Member;
import ca.yarbond.bookclub.repository.BookRepository;
import ca.yarbond.bookclub.repository.MemberRepository;
import ca.yarbond.bookclub.service.MemberQueueService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@Profile("!prod") // Only active in non-production environments
public class DataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final MemberQueueService memberQueueService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataInitializer(
            MemberRepository memberRepository,
            BookRepository bookRepository,
            MemberQueueService memberQueueService,
            PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.bookRepository = bookRepository;
        this.memberQueueService = memberQueueService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Only initialize if no members exist
        if (memberRepository.count() == 0) {
            // Add members in specified order with generic names
            // First member is admin
            Member member1 = createMember("Member1");
            member1.setRole(Member.Role.ADMIN);
            memberRepository.save(member1);
            
            Member member2 = createMember("Member2");
            Member member3 = createMember("Member3");
            Member member4 = createMember("Member4");
            Member member5 = createMember("Member5");
            Member inactiveMember = createInactiveMember("InactiveMember");

            // Set up queue in the specified order
            memberQueueService.addMemberToQueue(member1.getId());
            memberQueueService.addMemberToQueue(member2.getId());
            memberQueueService.addMemberToQueue(member3.getId());
            memberQueueService.addMemberToQueue(member4.getId());
            memberQueueService.addMemberToQueue(member5.getId());

            // Add a current book (Member1's book)
            Book currentBook = new Book();
            currentBook.setTitle("1984");
            currentBook.setAuthor("George Orwell");
            currentBook.setFiction(true);
            currentBook.setOwner(member1);
            currentBook.setStatus(BookStatus.CURRENT);
            currentBook.setComments("A dystopian novel set in a totalitarian society where government surveillance is omnipresent.");
            bookRepository.save(currentBook);

            // Add a completed book (Member5's book)
            Book completedBook = new Book();
            completedBook.setTitle("Thinking, Fast and Slow");
            completedBook.setAuthor("Daniel Kahneman");
            completedBook.setFiction(false);
            completedBook.setOwner(member5);
            completedBook.setStatus(BookStatus.COMPLETED);
            completedBook.setCompletionDate(LocalDate.now().minusMonths(1)); // Completed a month ago
            completedBook.setComments("A book about how the mind works and the two systems that drive the way we think.");
            bookRepository.save(completedBook);

            // Add some wishlist books for next member (Member2)
            Book wishlistBook1 = new Book();
            wishlistBook1.setTitle("Dune");
            wishlistBook1.setAuthor("Frank Herbert");
            wishlistBook1.setFiction(true);
            wishlistBook1.setOwner(member2);
            wishlistBook1.setStatus(BookStatus.WISHLIST);
            wishlistBook1.setComments("A science fiction novel about a desert planet with valuable resources.");
            bookRepository.save(wishlistBook1);

            Book wishlistBook2 = new Book();
            wishlistBook2.setTitle("Sapiens: A Brief History of Humankind");
            wishlistBook2.setAuthor("Yuval Noah Harari");
            wishlistBook2.setFiction(false);
            wishlistBook2.setOwner(member2);
            wishlistBook2.setStatus(BookStatus.WISHLIST);
            wishlistBook2.setComments("A book exploring the history and impact of humans on the world.");
            bookRepository.save(wishlistBook2);

            // Add some wishlist books for other members too
            Book wishlistBook3 = new Book();
            wishlistBook3.setTitle("The Three-Body Problem");
            wishlistBook3.setAuthor("Liu Cixin");
            wishlistBook3.setFiction(true);
            wishlistBook3.setOwner(member3);
            wishlistBook3.setStatus(BookStatus.WISHLIST);
            wishlistBook3.setComments("First book in a science fiction trilogy about humanity's first contact with aliens.");
            bookRepository.save(wishlistBook3);

            Book wishlistBook4 = new Book();
            wishlistBook4.setTitle("Atomic Habits");
            wishlistBook4.setAuthor("James Clear");
            wishlistBook4.setFiction(false);
            wishlistBook4.setOwner(member4);
            wishlistBook4.setStatus(BookStatus.WISHLIST);
            wishlistBook4.setComments("A practical guide to building good habits and breaking bad ones.");
            bookRepository.save(wishlistBook4);

            // Add wishlist book for inactive member
            Book wishlistBook5 = new Book();
            wishlistBook5.setTitle("Project Hail Mary");
            wishlistBook5.setAuthor("Andy Weir");
            wishlistBook5.setFiction(true);
            wishlistBook5.setOwner(inactiveMember);
            wishlistBook5.setStatus(BookStatus.WISHLIST);
            wishlistBook5.setComments("A science-based thriller about a lone astronaut who must save humanity.");
            bookRepository.save(wishlistBook5);
        }
    }

    private Member createMember(String name) {
        Member member = new Member();
        member.setName(name);
        member.setPasswordHash(passwordEncoder.encode(name + "123")); // Default password format: name123
        member.setRole(Member.Role.USER);
        return memberRepository.save(member);
    }

    private Member createInactiveMember(String name) {
        Member member = new Member();
        member.setName(name);
        member.setActive(false);
        member.setPasswordHash(passwordEncoder.encode(name + "123")); // Default password format: name123
        member.setRole(Member.Role.USER);
        return memberRepository.save(member);
    }
}