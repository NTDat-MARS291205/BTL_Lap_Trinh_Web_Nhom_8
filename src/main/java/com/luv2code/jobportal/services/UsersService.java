package com.luv2code.jobportal.services;

import com.luv2code.jobportal.entity.JobSeekerProfile;
import com.luv2code.jobportal.entity.RecruiterProfile;
import com.luv2code.jobportal.entity.Users;
import com.luv2code.jobportal.repository.JobSeekerProfileRepository;
import com.luv2code.jobportal.repository.RecruiterProfileRepository;
import com.luv2code.jobportal.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Service
public class UsersService {

    private final UsersRepository usersRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UsersService(UsersRepository usersRepository, JobSeekerProfileRepository jobSeekerProfileRepository, RecruiterProfileRepository recruiterProfileRepository, PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.jobSeekerProfileRepository = jobSeekerProfileRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Users addNew(Users users) {
        try {
            // ✅ Validate userTypeId
            if (users.getUserTypeId() == null) {
                throw new IllegalArgumentException("User type không được để trống");
            }

            // ✅ Set user properties
            users.setActive(true);
            users.setRegistrationDate(new Date(System.currentTimeMillis()));
            users.setPassword(passwordEncoder.encode(users.getPassword()));
            
            // ✅ Save user trước
            System.out.println("💾 Saving user: " + users.getEmail());
            Users savedUser = usersRepository.save(users);
            System.out.println("✅ User saved with ID: " + savedUser.getUserId());

            // ✅ Sau đó mới tạo profile
            int userTypeId = users.getUserTypeId().getUserTypeId();
            System.out.println("🔍 User type: " + userTypeId);

            try {
                if (userTypeId == 1) {
                    System.out.println("📋 Creating Recruiter Profile...");
                    RecruiterProfile recruiterProfile = new RecruiterProfile(savedUser);
                    recruiterProfileRepository.save(recruiterProfile);
                    System.out.println("✅ Recruiter profile created");
                }
                else if (userTypeId == 2) {
                    System.out.println("📋 Creating JobSeeker Profile...");
                    JobSeekerProfile jobSeekerProfile = new JobSeekerProfile(savedUser);
                    jobSeekerProfileRepository.save(jobSeekerProfile);
                    System.out.println("✅ JobSeeker profile created");
                }
            } catch (Exception profileException) {
                System.err.println("⚠️ Warning creating profile: " + profileException.getMessage());
                profileException.printStackTrace();
                // ⚠️ Profile creation failed nhưng user đã được tạo
                // Profile sẽ được tự động tạo khi getCurrentUserProfile() được gọi
                System.out.println("ℹ️ Profile will be created automatically on first login");
            }

            return savedUser;
            
        } catch (Exception e) {
            // Catch tất cả exception
            System.err.println("❌ Error in addNew(): " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi tạo tài khoản: " + e.getMessage(), e);
        }
    }

    public Object getCurrentUserProfile() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            String username = authentication.getName();
            Users users = usersRepository.findByEmail(username).orElseThrow(()-> new UsernameNotFoundException("Could not found " + "user"));
            int userId = users.getUserId();
            
            if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("Recruiter"))) {
                // ✅ Tìm hoặc tạo RecruiterProfile
                RecruiterProfile recruiterProfile = recruiterProfileRepository.findById(userId).orElse(null);
                if (recruiterProfile == null) {
                    System.out.println("⚠️ RecruiterProfile not found for user: " + username + ". Creating...");
                    recruiterProfile = new RecruiterProfile(users);
                    recruiterProfile = recruiterProfileRepository.save(recruiterProfile);
                    System.out.println("✅ RecruiterProfile created automatically");
                }
                return recruiterProfile;
            } else {
                // ✅ Tìm hoặc tạo JobSeekerProfile
                JobSeekerProfile jobSeekerProfile = jobSeekerProfileRepository.findById(userId).orElse(null);
                if (jobSeekerProfile == null) {
                    System.out.println("⚠️ JobSeekerProfile not found for user: " + username + ". Creating...");
                    jobSeekerProfile = new JobSeekerProfile(users);
                    jobSeekerProfile = jobSeekerProfileRepository.save(jobSeekerProfile);
                    System.out.println("✅ JobSeekerProfile created automatically");
                }
                return jobSeekerProfile;
            }
        }

        return null;
    }

    public Users getCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            String username = authentication.getName();
            Users user = usersRepository.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("Could not found " + "user"));
            return user;
        }

        return null;
    }

    public Users findByEmail(String currentUsername) {
        return usersRepository.findByEmail(currentUsername).orElseThrow(() -> new UsernameNotFoundException("User not " +
                "found"));
    }

    public Optional<Users> getUserByEmail(String email) {
        return usersRepository.findByEmail(email);
    }
    
}








