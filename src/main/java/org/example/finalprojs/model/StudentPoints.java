package org.example.finalprojs.model;

import jakarta.persistence.*;

@Entity
@Table(name = "student_points")
public class StudentPoints {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The student this balance belongs to
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Subject name e.g. "ITIM 2"
    @Column(nullable = false)
    private String subject;

    // Section e.g. "CP"
    @Column(nullable = false)
    private String section;

    // Points balance for this subject+section
    @Column(nullable = false)
    private int points = 0;

    public StudentPoints() {}

    public StudentPoints(User user, String subject, String section) {
        this.user    = user;
        this.subject = subject;
        this.section = section;
        this.points  = 0;
    }

    public Long getId()                     { return id; }
    public void setId(Long id)              { this.id = id; }

    public User getUser()                   { return user; }
    public void setUser(User user)          { this.user = user; }

    public String getSubject()              { return subject; }
    public void setSubject(String subject)  { this.subject = subject; }

    public String getSection()              { return section; }
    public void setSection(String section)  { this.section = section; }

    public int getPoints()                  { return points; }
    public void setPoints(int points)       { this.points = points; }
}