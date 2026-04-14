package com.code.jobportal.controller;

import com.code.jobportal.entity.Users;
import com.code.jobportal.entity.UsersType;
import com.code.jobportal.services.UsersService;
import com.code.jobportal.services.UsersTypeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class UsersController {

    private final UsersTypeService usersTypeService;
    private final UsersService usersService;

    @Autowired
    public UsersController(UsersTypeService usersTypeService, UsersService usersService) {
        this.usersTypeService = usersTypeService;
        this.usersService = usersService;
    }

    @GetMapping("/register")
    public String register(Model model) {
        List<UsersType> usersTypes = usersTypeService.getAll();
        model.addAttribute("getAllTypes", usersTypes);
        model.addAttribute("user", new Users());
        return "register";
    }

    @PostMapping("/register/new")
    public String userRegistration(@Valid Users users, Model model) {
        try {
            System.out.println("[Register] New user registration attempt: " + users.getEmail());
            
            // Kiểm tra email trùng
            Optional<Users> optionalUsers = usersService.getUserByEmail(users.getEmail());
            if (optionalUsers.isPresent()) {
                System.out.println("[Register] Email already exists: " + users.getEmail());
                model.addAttribute("error", "Email '" + users.getEmail() + "' đã được đăng ký. Vui lòng đăng nhập hoặc sử dụng email khác để tạo tài khoản mới.");
                List<UsersType> usersTypes = usersTypeService.getAll();
                model.addAttribute("getAllTypes", usersTypes);
                model.addAttribute("user", users);
                return "register";
            }

            // Kiểm tra userTypeId
            if (users.getUserTypeId() == null || users.getUserTypeId().getUserTypeId() == 0) {
                System.out.println("[Register] User type not selected");
                model.addAttribute("error", "Vui lòng chọn loại tài khoản (Ứng viên hoặc Nhà tuyển dụng).");
                List<UsersType> usersTypes = usersTypeService.getAll();
                model.addAttribute("getAllTypes", usersTypes);
                model.addAttribute("user", users);
                return "register";
            }

            System.out.println("[Register] Validation passed. Creating account...");
            // Đăng ký tài khoản
            Users newUser = usersService.addNew(users);
            System.out.println("[Register] Account created successfully for: " + users.getEmail() + " (ID: " + newUser.getUserId() + ")");
            
            model.addAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/login/";
            
        } catch (Exception e) {
            System.err.println("[Register] Exception during registration: " + e.getMessage());
            e.printStackTrace();
            
            model.addAttribute("error", "Lỗi đăng ký: " + e.getMessage() + ". Vui lòng thử lại.");
            List<UsersType> usersTypes = usersTypeService.getAll();
            model.addAttribute("getAllTypes", usersTypes);
            model.addAttribute("user", new Users());
            return "register";
        }
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }

        return "redirect:/";
    }

    /**
     * API endpoint để kiểm tra email trùng lặp (Real-time)
     * GET /api/check-email?email=test@example.com
     * Response: { "exists": true/false }
     */
    @GetMapping("/api/check-email")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkEmailExists(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        
        // Kiểm tra email trống
        if (email == null || email.trim().isEmpty()) {
            response.put("exists", false);
            response.put("message", "Email không được để trống");
            return ResponseEntity.badRequest().body(response);
        }
        
        // Tìm kiếm email trong database
        Optional<Users> existingUser = usersService.getUserByEmail(email.trim());
        
        if (existingUser.isPresent()) {
            response.put("exists", true);
            response.put("message", "Email này đã được đăng ký. Vui lòng sử dụng email khác.");
        } else {
            response.put("exists", false);
            response.put("message", "Email có thể sử dụng");
        }
        
        return ResponseEntity.ok(response);
    }
}
