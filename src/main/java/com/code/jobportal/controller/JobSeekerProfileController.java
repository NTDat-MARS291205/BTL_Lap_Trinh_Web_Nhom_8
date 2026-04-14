package com.code.jobportal.controller;

import com.code.jobportal.entity.JobSeekerProfile;
import com.code.jobportal.entity.Skills;
import com.code.jobportal.entity.Users;
import com.code.jobportal.repository.UsersRepository;
import com.code.jobportal.services.JobSeekerProfileService;
import com.code.jobportal.util.FileDownloadUtil;
import com.code.jobportal.util.FileUploadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Controller
@RequestMapping("/job-seeker-profile")
public class JobSeekerProfileController {

    private JobSeekerProfileService jobSeekerProfileService;

    private UsersRepository usersRepository;

    @Autowired
    public JobSeekerProfileController(JobSeekerProfileService jobSeekerProfileService, UsersRepository usersRepository) {
        this.jobSeekerProfileService = jobSeekerProfileService;
        this.usersRepository = usersRepository;
    }

    @GetMapping("/")
    public String jobSeekerProfile(Model model) {
        JobSeekerProfile jobSeekerProfile = new JobSeekerProfile();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<Skills> skills = new ArrayList<>();

        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            Users user = usersRepository.findByEmail(authentication.getName()).orElseThrow(() -> new UsernameNotFoundException("User not found."));
            Optional<JobSeekerProfile> seekerProfile = jobSeekerProfileService.getOne(user.getUserId());
            if (seekerProfile.isPresent()) {
                jobSeekerProfile = seekerProfile.get();
                if (jobSeekerProfile.getSkills().isEmpty()) {
                    skills.add(new Skills());
                    jobSeekerProfile.setSkills(skills);
                }
            }

            model.addAttribute("skills", skills);
            model.addAttribute("profile", jobSeekerProfile);
        }

        return "job-seeker-profile";
    }

    @PostMapping("/addNew")
    public String addNew(JobSeekerProfile jobSeekerProfile,
                         @RequestParam("image") MultipartFile image,
                         @RequestParam("pdf") MultipartFile pdf,
                         Model model) {
        try {
            System.out.println("📝 [JobSeeker Profile] Saving profile...");
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (!(authentication instanceof AnonymousAuthenticationToken)) {
                Users user = usersRepository.findByEmail(authentication.getName()).orElseThrow(() -> new UsernameNotFoundException("User not found."));
                jobSeekerProfile.setUserId(user);
                jobSeekerProfile.setUserAccountId(user.getUserId());
            }

            List<Skills> skillsList = new ArrayList<>();
            model.addAttribute("profile", jobSeekerProfile);
            model.addAttribute("skills", skillsList);

            for (Skills skills : jobSeekerProfile.getSkills()) {
                skills.setJobSeekerProfile(jobSeekerProfile);
            }

            String imageName = "";
            String resumeName = "";
            List<String> errors = new ArrayList<>();

            //  VALIDATION: Kiểm tra CV (bắt buộc)
            if (pdf.isEmpty() || Objects.equals(pdf.getOriginalFilename(), "")) {
                errors.add("CV không được để trống. Vui lòng upload file CV.");
            } else {
                resumeName = StringUtils.cleanPath(Objects.requireNonNull(pdf.getOriginalFilename()));
                jobSeekerProfile.setResume(resumeName);
            }

            // VALIDATION: Kiểm tra ảnh (bắt buộc)
            if (image.isEmpty() || Objects.equals(image.getOriginalFilename(), "")) {
                errors.add("Ảnh đại diện không được để trống. Vui lòng chọn ảnh.");
            } else {
                imageName = StringUtils.cleanPath(Objects.requireNonNull(image.getOriginalFilename()));
                jobSeekerProfile.setProfilePhoto(imageName);
            }

            // Nếu có lỗi validation → Trả về form với error messages
            if (!errors.isEmpty()) {
                System.out.println("[JobSeeker Profile] Validation errors: " + errors);
                model.addAttribute("errors", errors);
                JobSeekerProfile currentProfile = jobSeekerProfileService.getOne(jobSeekerProfile.getUserAccountId()).orElse(jobSeekerProfile);
                model.addAttribute("profile", currentProfile);
                return "job-seeker-profile";
            }

            JobSeekerProfile seekerProfile = jobSeekerProfileService.addNew(jobSeekerProfile);
            System.out.println("[JobSeeker Profile] Profile saved successfully");

            try {
                String uploadDir = "photos/candidate/" + jobSeekerProfile.getUserAccountId();
                if (!Objects.equals(image.getOriginalFilename(), "")) {
                    FileUploadUtil.saveFile(uploadDir, imageName, image);
                    System.out.println("[JobSeeker Profile] Image uploaded");
                }
                if (!Objects.equals(pdf.getOriginalFilename(), "")) {
                    FileUploadUtil.saveFile(uploadDir, resumeName, pdf);
                    System.out.println("[JobSeeker Profile] PDF uploaded");
                }
            }
            catch (IOException ex) {
                System.err.println("[JobSeeker Profile] File upload error: " + ex.getMessage());
                ex.printStackTrace();
                throw new RuntimeException("Lỗi upload file: " + ex.getMessage(), ex);
            }

            System.out.println("[JobSeeker Profile] Redirecting to /dashboard/");
            return "redirect:/dashboard/";
            
        } catch (Exception e) {
            System.err.println("[JobSeeker Profile] Exception: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errors", new ArrayList<>(java.util.Arrays.asList("Lỗi cập nhật: " + e.getMessage())));
            return "job-seeker-profile";
        }
    }

    @GetMapping("/{id}")
    public String candidateProfile(@PathVariable("id") int id, Model model) {

        Optional<JobSeekerProfile> seekerProfile = jobSeekerProfileService.getOne(id);
        model.addAttribute("profile", seekerProfile.get());
        return "job-seeker-profile";
    }

    @GetMapping("/downloadResume")
    public ResponseEntity<?> downloadResume(@RequestParam(value = "fileName") String fileName, @RequestParam(value = "userID") String userId) {

        FileDownloadUtil downloadUtil = new FileDownloadUtil();
        Resource resource = null;

        try {
            resource = downloadUtil.getFileAsResourse("photos/candidate/" + userId, fileName);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }

        if (resource == null) {
            return new ResponseEntity<>("File not found", HttpStatus.NOT_FOUND);
        }

        String contentType = "application/octet-stream";
        String headerValue = "attachment; filename=\"" + resource.getFilename() + "\"";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
                .body(resource);

    }
}











