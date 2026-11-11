package com.college.management.controller;

import com.college.management.model.*;
import com.college.management.repository.*;
import com.college.management.service.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CollegeController {
    private final UserRepository users;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final CurrentUserService current;

    public CollegeController(UserRepository users, CourseRepository courses,
                             EnrollmentRepository enrollments, CurrentUserService current) {
        this.users = users; this.courses = courses; this.enrollments = enrollments; this.current = current;
    }

    public record UserRequest(@NotBlank String name, @NotBlank @Email String email,
                              String password, String rollNumber, String department, String phone) {}
    public record CourseRequest(@NotBlank String title, @NotBlank String code, String description,
                                @Min(1) @Max(10) Integer credits, @Min(1) Integer capacity,
                                Long teacherId) {}

    @GetMapping("/courses")
    public List<Map<String,Object>> listCourses() {
        AppUser me = current.get();
        List<Course> list = me.getRole() == Role.TEACHER && !current.isAdmin()
            ? courses.findByTeacherId(me.getId()) : courses.findAll();
        return list.stream().map(this::courseDto).toList();
    }

    @PostMapping("/courses")
    public ResponseEntity<?> addCourse(@Valid @RequestBody CourseRequest req) {
        AppUser me = current.get();
        if (me.getRole() != Role.TEACHER && me.getRole() != Role.ADMIN) throw new AccessDeniedException("Teacher or admin only");
        if (courses.existsByCode(req.code())) return ResponseEntity.badRequest().body(Map.of("message","Course code already exists"));
        AppUser teacher;
        if (me.getRole() == Role.ADMIN) {
            teacher = req.teacherId() != null
                ? users.findById(req.teacherId()).orElseThrow(() -> new RuntimeException("Teacher not found"))
                : users.findByRole(Role.TEACHER).stream().findFirst().orElse(null);
        } else {
            teacher = me;
        }
        if (teacher == null || teacher.getRole() != Role.TEACHER)
            return ResponseEntity.badRequest().body(Map.of("message","Create a teacher account before adding a course"));
        Course c = new Course(); fillCourse(c, req, teacher); courses.save(c);
        return ResponseEntity.status(HttpStatus.CREATED).body(courseDto(c));
    }

    @PutMapping("/courses/{id}")
    public ResponseEntity<?> updateCourse(@PathVariable Long id, @Valid @RequestBody CourseRequest req) {
        Course c = courses.findById(id).orElseThrow(() -> new RuntimeException("Course not found"));
        AppUser me = current.get(); ensureCourseOwner(c, me);
        if (!c.getCode().equals(req.code()) && courses.existsByCode(req.code()))
            return ResponseEntity.badRequest().body(Map.of("message","Course code already exists"));
        AppUser teacher = me.getRole() == Role.ADMIN && req.teacherId() != null
            ? users.findById(req.teacherId()).orElseThrow() : c.getTeacher();
        fillCourse(c, req, teacher); courses.save(c);
        return ResponseEntity.ok(courseDto(c));
    }

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id) {
        Course c = courses.findById(id).orElseThrow(() -> new RuntimeException("Course not found"));
        ensureCourseOwner(c, current.get());
        enrollments.deleteByCourseId(id); courses.delete(c);
        return ResponseEntity.ok(Map.of("message","Course deleted"));
    }

    @GetMapping("/students")
    public List<Map<String,Object>> students() {
        return users.findByRole(Role.STUDENT).stream().map(AuthController::safe).toList();
    }

    @PostMapping("/students")
    public ResponseEntity<?> addStudent(@Valid @RequestBody UserRequest req) {
        if (users.existsByEmail(req.email())) return ResponseEntity.badRequest().body(Map.of("message","Email already exists"));
        if (req.rollNumber() != null && users.existsByRollNumber(req.rollNumber()))
            return ResponseEntity.badRequest().body(Map.of("message","Roll number already exists"));
        AppUser u = new AppUser(); u.setName(req.name()); u.setEmail(req.email());
        u.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(
            req.password() == null || req.password().isBlank() ? "Student@123" : req.password()));
        u.setRole(Role.STUDENT); u.setRollNumber(req.rollNumber()); u.setDepartment(req.department()); u.setPhone(req.phone());
        users.save(u); return ResponseEntity.status(HttpStatus.CREATED).body(AuthController.safe(u));
    }

    @PutMapping("/students/{id}")
    public ResponseEntity<?> updateStudent(@PathVariable Long id, @Valid @RequestBody UserRequest req) {
        AppUser u = users.findById(id).orElseThrow(() -> new RuntimeException("Student not found"));
        if (u.getRole() != Role.STUDENT) throw new RuntimeException("Student not found");
        if (!u.getEmail().equals(req.email()) && users.existsByEmail(req.email()))
            return ResponseEntity.badRequest().body(Map.of("message","Email already exists"));
        u.setName(req.name()); u.setEmail(req.email()); u.setRollNumber(req.rollNumber());
        u.setDepartment(req.department()); u.setPhone(req.phone());
        if (req.password() != null && !req.password().isBlank())
            u.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(req.password()));
        users.save(u); return ResponseEntity.ok(AuthController.safe(u));
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<?> deleteStudent(@PathVariable Long id) {
        AppUser u = users.findById(id).orElseThrow(() -> new RuntimeException("Student not found"));
        if (u.getRole() != Role.STUDENT) throw new RuntimeException("Student not found");
        enrollments.deleteByStudentId(id); users.delete(u);
        return ResponseEntity.ok(Map.of("message","Student deleted"));
    }

    @GetMapping("/teachers")
    public List<Map<String,Object>> teachers() {
        return users.findByRole(Role.TEACHER).stream().map(AuthController::safe).toList();
    }

    @PostMapping("/admin/teachers")
    public ResponseEntity<?> addTeacher(@Valid @RequestBody UserRequest req) {
        if (users.existsByEmail(req.email())) return ResponseEntity.badRequest().body(Map.of("message","Email already exists"));
        AppUser u = new AppUser(); u.setName(req.name()); u.setEmail(req.email());
        u.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(
            req.password() == null || req.password().isBlank() ? "Teacher@123" : req.password()));
        u.setRole(Role.TEACHER); u.setDepartment(req.department()); u.setPhone(req.phone());
        users.save(u); return ResponseEntity.status(HttpStatus.CREATED).body(AuthController.safe(u));
    }

    @DeleteMapping("/admin/teachers/{id}")
    public ResponseEntity<?> deleteTeacher(@PathVariable Long id) {
        AppUser teacher = users.findById(id).orElseThrow();
        if (teacher.getRole() != Role.TEACHER) throw new RuntimeException("Teacher not found");
        List<Course> owned = courses.findByTeacherId(id);
        for (Course c : owned) { enrollments.deleteByCourseId(c.getId()); courses.delete(c); }
        users.delete(teacher); return ResponseEntity.ok(Map.of("message","Teacher and their courses deleted"));
    }

    @PostMapping("/enrollments/{courseId}")
    public ResponseEntity<?> enroll(@PathVariable Long courseId) {
        AppUser student = current.get();
        if (student.getRole() != Role.STUDENT) throw new AccessDeniedException("Students only");
        Course course = courses.findById(courseId).orElseThrow(() -> new RuntimeException("Course not found"));
        if (enrollments.existsByStudentIdAndCourseId(student.getId(), courseId))
            return ResponseEntity.badRequest().body(Map.of("message","Already enrolled in this course"));
        if (course.getCapacity() != null && enrollments.countByCourseId(courseId) >= course.getCapacity())
            return ResponseEntity.badRequest().body(Map.of("message","Course is full"));
        Enrollment e = new Enrollment(); e.setStudent(student); e.setCourse(course); enrollments.save(e);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message","Enrollment successful"));
    }

    @DeleteMapping("/enrollments/{courseId}")
    public ResponseEntity<?> cancelEnrollment(@PathVariable Long courseId) {
        AppUser student = current.get();
        if (student.getRole() != Role.STUDENT) throw new AccessDeniedException("Students only");
        Enrollment e = enrollments.findByStudentId(student.getId()).stream()
            .filter(x -> x.getCourse().getId().equals(courseId)).findFirst()
            .orElseThrow(() -> new RuntimeException("Enrollment not found"));
        enrollments.delete(e); return ResponseEntity.ok(Map.of("message","Enrollment cancelled"));
    }

    @GetMapping("/enrollments/me")
    public List<Map<String,Object>> myEnrollments() {
        AppUser student = current.get();
        if (student.getRole() != Role.STUDENT) throw new AccessDeniedException("Students only");
        return enrollments.findByStudentId(student.getId()).stream().map(this::enrollmentDto).toList();
    }

    @GetMapping("/courses/{courseId}/enrollments")
    public List<Map<String,Object>> courseEnrollments(@PathVariable Long courseId) {
        Course course = courses.findById(courseId).orElseThrow();
        ensureCourseOwner(course, current.get());
        return enrollments.findByCourseId(courseId).stream().map(this::enrollmentDto).toList();
    }

    @GetMapping("/dashboard/summary")
    public Map<String,Object> summary() {
        AppUser me = current.get();
        Map<String,Object> result = new LinkedHashMap<>();
        if (me.getRole() == Role.ADMIN) {
            result.put("students", users.findByRole(Role.STUDENT).size());
            result.put("teachers", users.findByRole(Role.TEACHER).size());
            result.put("courses", courses.count()); result.put("enrollments", enrollments.count());
        } else if (me.getRole() == Role.TEACHER) {
            List<Course> own = courses.findByTeacherId(me.getId());
            result.put("courses", own.size());
            result.put("students", own.stream().mapToLong(c -> enrollments.countByCourseId(c.getId())).sum());
            result.put("courseCounts", own.stream().map(c -> {
                Map<String,Object> m = new LinkedHashMap<>(); m.put("course", c.getTitle());
                m.put("code", c.getCode()); m.put("enrolled", enrollments.countByCourseId(c.getId())); return m;
            }).toList());
        } else {
            result.put("availableCourses", courses.count());
            result.put("myEnrollments", enrollments.findByStudentId(me.getId()).size());
        }
        result.put("role", me.getRole().name()); result.put("name", me.getName());
        return result;
    }

    private void ensureCourseOwner(Course course, AppUser me) {
        if (me.getRole() != Role.ADMIN &&
            !(me.getRole() == Role.TEACHER && course.getTeacher().getId().equals(me.getId())))
            throw new AccessDeniedException("You can manage only your own courses");
    }
    private void fillCourse(Course c, CourseRequest r, AppUser teacher) {
        c.setTitle(r.title()); c.setCode(r.code()); c.setDescription(r.description());
        c.setCredits(r.credits() == null ? 3 : r.credits());
        c.setCapacity(r.capacity() == null ? 60 : r.capacity()); c.setTeacher(teacher);
    }
    private Map<String,Object> courseDto(Course c) {
        Map<String,Object> m = new LinkedHashMap<>(); m.put("id", c.getId()); m.put("title", c.getTitle());
        m.put("code", c.getCode()); m.put("description", c.getDescription()); m.put("credits", c.getCredits());
        m.put("capacity", c.getCapacity()); m.put("teacherName", c.getTeacher().getName());
        m.put("teacherId", c.getTeacher().getId()); m.put("enrolledCount", enrollments.countByCourseId(c.getId()));
        return m;
    }
    private Map<String,Object> enrollmentDto(Enrollment e) {
        Map<String,Object> m = new LinkedHashMap<>(); m.put("id", e.getId());
        m.put("student", AuthController.safe(e.getStudent())); m.put("course", courseDto(e.getCourse()));
        m.put("enrolledAt", e.getEnrolledAt()); return m;
    }
}
