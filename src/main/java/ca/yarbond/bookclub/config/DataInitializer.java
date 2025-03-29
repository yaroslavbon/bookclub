package ca.yarbond.bookclub.config;

import ca.yarbond.bookclub.model.Book;
import ca.yarbond.bookclub.model.BookStatus;
import ca.yarbond.bookclub.model.Member;
import ca.yarbond.bookclub.repository.BookRepository;
import ca.yarbond.bookclub.repository.MemberRepository;
import ca.yarbond.bookclub.service.MemberQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final MemberQueueService memberQueueService;

    @Autowired
    public DataInitializer(
            MemberRepository memberRepository,
            BookRepository bookRepository,
            MemberQueueService memberQueueService) {
        this.memberRepository = memberRepository;
        this.bookRepository = bookRepository;
        this.memberQueueService = memberQueueService;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Only initialize if no members exist
        if (memberRepository.count() == 0) {
            // Add members in specified order
            Member alik = createMember("Alik");
            Member max = createMember("Max");
            Member vadim = createMember("Vadim");
            Member yar = createMember("Yar");
            Member alex = createMember("Alex");

            // Set up queue in the specified order
            memberQueueService.addMemberToQueue(alik.getId());
            memberQueueService.addMemberToQueue(max.getId());
            memberQueueService.addMemberToQueue(vadim.getId());
            memberQueueService.addMemberToQueue(yar.getId());
            memberQueueService.addMemberToQueue(alex.getId());

            // Add "Clockwork Orange" as current book (Alik's book)
            Book clockworkOrange = new Book();
            clockworkOrange.setTitle("A Clockwork Orange");
            clockworkOrange.setAuthor("Anthony Burgess");
            clockworkOrange.setFiction(true);
            clockworkOrange.setOwner(alik);
            clockworkOrange.setStatus(BookStatus.CURRENT);
            clockworkOrange.setComments("A dystopian satirical black comedy novel exploring the violent nature of humans.");
            bookRepository.save(clockworkOrange);

            // Add "Норма" as completed book (Alex's book)
            Book norma = new Book();
            norma.setTitle("Норма");
            norma.setAuthor("Владимир Сорокин");
            norma.setFiction(true);
            norma.setOwner(alex);
            norma.setStatus(BookStatus.COMPLETED);
            norma.setCompletionDate(LocalDate.now().minusMonths(1)); // Completed a month ago
            norma.setComments("A postmodern novel by Vladimir Sorokin.");
            bookRepository.save(norma);

            // Add some wishlist books for next member (Max)
            Book book1 = new Book();
            book1.setTitle("Dune");
            book1.setAuthor("Frank Herbert");
            book1.setFiction(true);
            book1.setOwner(max);
            book1.setStatus(BookStatus.WISHLIST);
            book1.setComments("A science fiction novel about a desert planet.");
            bookRepository.save(book1);

            Book book2 = new Book();
            book2.setTitle("Thinking, Fast and Slow");
            book2.setAuthor("Daniel Kahneman");
            book2.setFiction(false);
            book2.setOwner(max);
            book2.setStatus(BookStatus.WISHLIST);
            book2.setComments("A book about how we think and make decisions.");
            bookRepository.save(book2);

            // Add some wishlist books for other members too
            Book book3 = new Book();
            book3.setTitle("The Three-Body Problem");
            book3.setAuthor("Liu Cixin");
            book3.setFiction(true);
            book3.setOwner(vadim);
            book3.setStatus(BookStatus.WISHLIST);
            book3.setComments("First book in a science fiction trilogy about humanity's first contact with aliens.");
            bookRepository.save(book3);

            Book book4 = new Book();
            book4.setTitle("Sapiens: A Brief History of Humankind");
            book4.setAuthor("Yuval Noah Harari");
            book4.setFiction(false);
            book4.setOwner(yar);
            book4.setStatus(BookStatus.WISHLIST);
            book4.setComments("A book about the history and impact of Homo sapiens.");
            bookRepository.save(book4);
        }
    }

    private Member createMember(String name) {
        Member member = new Member();
        member.setName(name);
        return memberRepository.save(member);
    }
}