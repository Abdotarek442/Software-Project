package com.lms.controller;


import com.lms.domain.dto.BasicResponseDto;
import com.lms.domain.dto.course.AssignmentDto;
import com.lms.domain.dto.course.MaterialTransferDto;
import com.lms.domain.dto.course.SetGradeDto;
import com.lms.domain.execptionhandler.UnauthorizedAccessException;
import com.lms.domain.model.course.AssignmentSubmission;
import com.lms.domain.model.course.Course;
import com.lms.domain.model.user.Roles;
import com.lms.domain.service.AssignmentSubmissionService;
import com.lms.domain.service.CourseService;
import com.lms.domain.service.UserService;
import org.apache.catalina.connector.Response;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.function.EntityResponse;

import java.security.Principal;

@Controller
@RequestMapping("/courses/{courseId}/assignments/{aId}")
public class AssignmentSubmissionController {

    AssignmentSubmissionService assignmentSubmissionService;
    UserService userService;
    CourseService courseService;

    public AssignmentSubmissionController(AssignmentSubmissionService assignmentSubmissionService, UserService userService, CourseService courseService) {
        this.assignmentSubmissionService = assignmentSubmissionService;
        this.userService = userService;
        this.courseService = courseService;
    }

    @PostMapping("/submit")
    public ResponseEntity<?> SubmitAssignment(@PathVariable Long aId, @RequestBody MultipartFile file) {
        Long userId = userService.getCurrentUserId();
        assignmentSubmissionService.SubmitAssignment(userId,aId,file);
        return ResponseEntity.status(HttpStatus.OK).body(new BasicResponseDto(
                "success", "Assignment submitted successfully"
        ));
    }

    @GetMapping("/submission/{subId}")
    public ResponseEntity<?> getSubmission(@PathVariable Long subId) {
        MaterialTransferDto materialTransferDto = assignmentSubmissionService.getSubmission(subId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(materialTransferDto.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + materialTransferDto.getName() + "\"")
                .body(materialTransferDto.getResource());
    }

    @PostMapping("/submission/{subId}/grade")
    public ResponseEntity<?> gradeAssignment(@PathVariable Long subId, @RequestBody SetGradeDto grade) {
        if (grade.getGrade() < 0 || grade.getGrade() > 100) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new BasicResponseDto(
                    "error", "Invalid grade. Grade must be between 0 and 100."
            ));
        }
        assignmentSubmissionService.gradeAssignment(subId,grade);
        return ResponseEntity.status(HttpStatus.OK).body(new BasicResponseDto(
                "success", "Assignment Successfully graded"
        ));
    }

    @GetMapping("/submission/{subId}/grade")
    public ResponseEntity<?> getGrade(@PathVariable Long subId, @PathVariable Long courseId) {
        if(userService.getCurrentUserRole() == Roles.ROLE_INSTRUCTOR &&
                !courseService.isInstructing(userService.getCurrentUserId() ,courseId)) {
            throw new UnauthorizedAccessException();
        }

        SetGradeDto grade = assignmentSubmissionService.getGrade(subId);
        return ResponseEntity.status(HttpStatus.OK).body(grade);
    }

}
