package ca.yarbond.bookclub.model;

import lombok.Data;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Table(name = "member_queue")
@Data
public class MemberQueueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Member member;

    @Column(nullable = false)
    private int position;
}