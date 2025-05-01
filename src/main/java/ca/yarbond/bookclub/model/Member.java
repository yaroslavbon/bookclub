package ca.yarbond.bookclub.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "members")
@Data
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // Used as username for authentication

    // Authentication fields
    @Column(nullable = false)
    private String passwordHash;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;
    
    @OneToMany(mappedBy = "owner")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private List<Book> books = new ArrayList<>();

    @OneToMany(mappedBy = "member")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private List<Rating> ratings = new ArrayList<>();

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "last_pick_date")
    private LocalDate lastPickDate;

    @Column(name = "total_picks")
    private int totalPicks;

    public void incrementTotalPicks(){
        totalPicks++;
    }
    
    // Role enum for authorization
    public enum Role {
        ADMIN,
        USER
    }
}