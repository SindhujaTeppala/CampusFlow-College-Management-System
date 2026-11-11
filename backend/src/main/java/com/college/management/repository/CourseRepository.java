package com.college.management.repository;
import com.college.management.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CourseRepository extends JpaRepository<Course, Long> {
    boolean existsByCode(String code);
    List<Course> findByTeacherId(Long teacherId);
    Optional<Course> findByIdAndTeacherId(Long id, Long teacherId);
}
