package org.example.finalprojs.controller;

import org.example.finalprojs.model.TeacherClass;
import org.example.finalprojs.repository.TeacherClassRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
@CrossOrigin(origins = "*")
public class TeacherClassController {

    private final TeacherClassRepository teacherClassRepository;

    @Autowired
    public TeacherClassController(TeacherClassRepository teacherClassRepository) {
        this.teacherClassRepository = teacherClassRepository;
    }

    @GetMapping("/teacher/{teacherId}")
    public List<TeacherClass> getClassesByTeacher(@PathVariable Long teacherId) {
        return teacherClassRepository.findByTeacherId(teacherId);
    }
}