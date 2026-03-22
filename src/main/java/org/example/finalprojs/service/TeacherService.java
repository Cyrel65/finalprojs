package org.example.finalprojs.service;

import org.example.finalprojs.model.Teacher;
import org.example.finalprojs.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TeacherService {

    private final TeacherRepository teacherRepository;

    @Autowired
    public TeacherService(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    public Teacher register(Teacher teacher) {
        if (teacherRepository.existsByEmail(teacher.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        return teacherRepository.save(teacher);
    }

    public Optional<Teacher> login(String email, String password) {
        return teacherRepository.findByEmail(email)
                .filter(t -> t.getPassword().equals(password));
    }

    public Optional<Teacher> findTeacherById(String id) {
        try {
            Long longId = Long.valueOf(id);
            return teacherRepository.findById(longId);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public Optional<Teacher> findTeacherByEmail(String email) {
        return teacherRepository.findByEmail(email);
    }

    public void updateResetToken(Long teacherId, String token) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
        teacher.setResetToken(token);
        teacherRepository.save(teacher);
    }

    public void updatePassword(String token, String newPassword) {
        // Find teacher by the token we sent to their email
        Teacher teacher = teacherRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        teacher.setPassword(newPassword); // Saving as plain text
        teacher.setResetToken(null); // Clear token so it can't be used again
        teacherRepository.save(teacher);
    }
}