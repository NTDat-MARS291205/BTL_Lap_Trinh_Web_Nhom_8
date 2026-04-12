# 🔐 FEATURE: Thông Báo Lỗi Đăng Nhập

---

## 🎯 TÍNH NĂNG

Khi người dùng **đăng nhập sai email hoặc mật khẩu**, hệ thống sẽ:
- ✅ Hiển thị **red alert box** rõ ràng
- ✅ Thông báo: "Email hoặc mật khẩu không chính xác"
- ✅ Cho phép dismiss (nút ✖️)
- ✅ Có icon ❌ cho rõ ràng

---

## 🔄 HOẠT ĐỘNG

### Quy Trình Đăng Nhập Sai

```
1. User nhập email sai hoặc mật khẩu sai
   ↓
2. Click "Đăng nhập"
   ↓
3. Spring Security xác thực thất bại
   ↓
4. Redirect: /login?error
   ↓
5. Template check: th:if="${param.error}"
   ↓
6. Hiển thị red alert box:
   ❌ Đăng nhập thất bại!
   Email hoặc mật khẩu không chính xác. Vui lòng thử lại.
   ↓
7. User thấy ngay ✅
```

### Quy Trình Đăng Nhập Đúng

```
1. User nhập email đúng + mật khẩu đúng
   ↓
2. Click "Đăng nhập"
   ↓
3. Spring Security xác thực thành công
   ↓
4. Gọi CustomAuthenticationSuccessHandler
   ↓
5. Redirect dựa trên role:
   - Job Seeker → /dashboard/
   - Recruiter → /dashboard/
```

---

## 🎨 GIAO DIỆN

### Alert Box Khi Lỗi

```
┌─────────────────────────────────────────────────┐
│ ❌ Đăng nhập thất bại!                          │
│ Email hoặc mật khẩu không chính xác. Vui lòng  │
│ thử lại.                                    ✖️  │
└─────────────────────────────────────────────────┘

🔴 Red background (#dc3545)
⚠️ Icon to attract attention
✖️ Dismiss button (nút đóng)
```

### Alert Box Khi Đăng Ký Thành Công

```
┌─────────────────────────────────────────────────┐
│ ✅ Đăng ký thành công!                          │
│ Vui lòng đăng nhập bằng email và mật khẩu của  │
│ bạn.                                        ✖️  │
└─────────────────────────────────────────────────┘

🟢 Green background (#28a745)
✓ Check icon
✖️ Dismiss button
```

---

## 🔧 IMPLEMENTATION

### login.html - Error Alert

```html
<!-- ✅ Hiển thị error message khi đăng nhập sai -->
<div th:if="${param.error}" class="alert alert-danger alert-dismissible fade show" 
     role="alert" style="border-radius: 8px; border-left: 4px solid #dc3545; margin-bottom: 20px;">
    <div style="display: flex; align-items: center; gap: 10px;">
        <i class="fa-solid fa-circle-xmark" style="font-size: 20px;"></i>
        <div>
            <strong>❌ Đăng nhập thất bại!</strong>
            <br/>
            <span style="font-size: 14px;">
                Email hoặc mật khẩu không chính xác. Vui lòng thử lại.
            </span>
        </div>
    </div>
    <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
</div>
```

### login.html - Success Alert (Từ /register redirect)

```html
<!-- ✅ Hiển thị success message khi đăng ký thành công -->
<div th:if="${param.success}" class="alert alert-success alert-dismissible fade show" 
     role="alert" style="border-radius: 8px; border-left: 4px solid #28a745; margin-bottom: 20px;">
    <div style="display: flex; align-items: center; gap: 10px;">
        <i class="fa-solid fa-circle-check" style="font-size: 20px;"></i>
        <div>
            <strong>✅ Đăng ký thành công!</strong>
            <br/>
            <span style="font-size: 14px;">
                Vui lòng đăng nhập bằng email và mật khẩu của bạn.
            </span>
        </div>
    </div>
    <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
</div>
```

---

## 💡 CÁC PARAMETER

### Spring Security Automatic Parameters

```
/login?error
├─ Kích hoạt khi:
│  ├─ Email không đúng
│  ├─ Password không đúng
│  └─ User bị disable

/login?success
├─ Custom parameter (có thể add từ redirect)
└─ Dùng để hiển thị success message
```

### Cách Sử Dụng Trong Thymeleaf

```html
<!-- Kiểm tra error parameter -->
<div th:if="${param.error}">
    <!-- Hiển thị khi URL có ?error -->
</div>

<!-- Kiểm tra success parameter -->
<div th:if="${param.success}">
    <!-- Hiển thị khi URL có ?success -->
</div>
```

---

## 🧪 TESTING

### Test 1: Email Sai

```
1. Vào /login
2. Email: nonexistent@example.com
3. Password: 12345678
4. Click "Đăng nhập"

Expected:
🔴 Red alert box hiển thị:
   ❌ Đăng nhập thất bại!
   Email hoặc mật khẩu không chính xác...

URL: /login?error
```

---

### Test 2: Mật Khẩu Sai

```
1. Vào /login
2. Email: admin@example.com (email tồn tại)
3. Password: wrongpassword123
4. Click "Đăng nhập"

Expected:
🔴 Red alert box hiển thị:
   ❌ Đăng nhập thất bại!
   Email hoặc mật khẩu không chính xác...

URL: /login?error
```

---

### Test 3: Đúng Email & Mật Khẩu

```
1. Vào /login
2. Email: admin@example.com
3. Password: correct_password
4. Click "Đăng nhập"

Expected:
✅ Redirect /dashboard/
✅ Người dùng đã authenticated
```

---

### Test 4: Đăng Ký Thành Công

```
1. Vào /register
2. Đăng ký tài khoản mới
3. Redirect /login?success

Expected:
🟢 Green alert box hiển thị:
   ✅ Đăng ký thành công!
   Vui lòng đăng nhập bằng email...
```

---

## 🔐 SECURITY

### Thông Báo Chung Chung (Bảo Mật)

```
❌ ĐỨ: "Email không tồn tại"
   (Lộ ra thông tin email tồn tại hay không)

✅ ĐÚNG: "Email hoặc mật khẩu không chính xác"
   (Không lộ thông tin)
```

**Tại sao?**
- Attackers không thể biết email nào tồn tại
- Bảo vệ privacy người dùng
- Best practice security

---

## 📁 FILES UPDATED

| File | Change |
|------|--------|
| **login.html** | ✏️ Thêm error & success alert boxes |
| **WebSecurityConfig.java** | ✅ Đã sẵn có (không cần thay đổi) |

---

## 🎯 HOW IT WORKS

### 1. Spring Security Failed Login

```java
// WebSecurityConfig.java
http.formLogin(form->form
    .loginPage("/login")     // Redirect tới /login khi fail
    .permitAll()
    .successHandler(...)     // Custom handler khi success
)
```

### 2. Failed Login → /login?error

```
Authentication Fail
    ↓
Spring Security redirect
    ↓
/login?error
    ↓
Thymeleaf check ${param.error}
    ↓
Display alert box
```

### 3. Success Login → Dashboard

```
Authentication Success
    ↓
Call CustomAuthenticationSuccessHandler
    ↓
Redirect to /dashboard/
    ↓
User logged in
```

---

## ✅ RESULT

- ✅ Lỗi đăng nhập → Red alert box
- ✅ Success register → Green alert box (redirect)
- ✅ Rõ ràng, dễ hiểu
- ✅ Dismissible alerts
- ✅ Security best practice

---

**Ngày cập nhật: 12/04/2026**  
**Status: ✅ IMPLEMENTED**

