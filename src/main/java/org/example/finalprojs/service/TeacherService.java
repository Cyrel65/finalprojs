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
}