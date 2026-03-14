package org.example.finalprojs.model;

import jakarta.persistence.*;

@Entity
@Table(name = "teacher_classes")
public class TeacherClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String subject;
    private String section;
    private String startTime;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    public TeacherClass() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher teacher) { this.teacher = teacher; }
}