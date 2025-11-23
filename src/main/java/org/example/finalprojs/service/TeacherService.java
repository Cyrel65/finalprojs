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

    /**
     * Finds a Teacher by their ID (used when a String ID is read from the message table).
     */
    public Optional<Teacher> findTeacherById(String id) {
        try {
            Long longId = Long.valueOf(id);
            return teacherRepository.findById(longId);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Finds a Teacher by their email (used when validating a recipient email).
     */
    public Optional<Teacher> findTeacherByEmail(String email) {
        return teacherRepository.findByEmail(email);
    }
}