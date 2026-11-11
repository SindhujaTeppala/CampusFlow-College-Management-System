package com.college.management.repository;
import com.college.management.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
    long countByCourseId(Long courseId);
    List<Enrollment> findByStudentId(Long studentId);
    List<Enrollment> findByCourseId(Long courseId);
    List<Enrollment> findByCourseTeacherId(Long teacherId);
    void deleteByCourseId(Long courseId);
    void deleteByStudentId(Long studentId);
}
