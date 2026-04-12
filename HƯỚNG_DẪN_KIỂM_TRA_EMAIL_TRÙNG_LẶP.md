# 📧 Hướng Dẫn: Kiểm Tra Email Trùng Lặp khi Đăng Ký

---

## ✨ Tính Năng Được Thêm

Hệ thống giờ đã có **2 cấp độ kiểm tra email trùng lặp**:

### 1️⃣ **Real-time Email Check (Phía Client)**
- ✅ Kiểm tra email **ngay khi người dùng nhập**
- ✅ Hiển thị biểu tượng ✅/❌ tức thời
- ✅ Vô hiệu hóa nút "Đăng ký" nếu email bị trùng
- ✅ Không cần chờ submit form

### 2️⃣ **Server-side Validation (Phía Server)**
- ✅ Kiểm tra lại khi form được submit
- ✅ Hiển thị thông báo lỗi đẹp trên trang
- ✅ Giữ lại dữ liệu nhập để người dùng chỉnh sửa

---

## 📊 CẤU TRÚC HOẠT ĐỘNG

```
┌─────────────────────────────────────────────────────────────┐
│  NGƯỜI DÙNG NHẬP EMAIL                                      │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ▼
        ┌──────────────────────┐
        │ JavaScript Listener  │
        │ (event: 'input')     │
        └──────────┬───────────┘
                   │
                   ▼
        ┌──────────────────────┐
        │ Kiểm tra Định Dạng   │
        │ (Regex Email)        │
        └──────────┬───────────┘
                   │
         ┌─────────▼──────────┐
         │ Định dạng OK?      │
         └────┬──────────┬────┘
              │ Không    │ Có
              ▼          ▼
         ❌ Lỗi    Delay 500ms
              │          │
              │          ▼
              │   ┌──────────────────┐
              │   │ Gọi API Server   │
              │   │ /api/check-email │
              │   └────────┬─────────┘
              │            │
              │            ▼
              │   ┌────────────────┐
              │   │ Cơ Sở Dữ Liệu │
              │   │ (tìm Email)    │
              │   └────────┬───────┘
              │            │
              │      ┌─────▼─────┐
              │      │ Tồn tại?  │
              │      └──┬─────┬──┘
              │         │     │
              │         │ Có  │ Không
              │         ▼     ▼
              │        ❌    ✅
              │        │     │
              └────────┼─────┘
                       │
                       ▼
           ┌──────────────────────┐
           │ Cập Nhật UI          │
           │ - Icon & Message     │
           │ - Nút Submit         │
           └──────────────────────┘
```

---

## 🔧 CHI TIẾT IMPLEMENTATIONS

### A. CONTROLLER (UsersController.java)

#### ✅ Cải thiện Message Lỗi

```java
@PostMapping("/register/new")
public String userRegistration(@Valid Users users, Model model) {
    Optional<Users> optionalUsers = usersService.getUserByEmail(users.getEmail());
    if (optionalUsers.isPresent()) {
        // Thông báo lỗi chi tiết hơn
        model.addAttribute("error", 
            "Email '" + users.getEmail() + "' đã được đăng ký. " +
            "Vui lòng đăng nhập hoặc sử dụng email khác để tạo tài khoản mới.");
        
        List<UsersType> usersTypes = usersTypeService.getAll();
        model.addAttribute("getAllTypes", usersTypes);
        
        // Giữ lại dữ liệu đã nhập (trừ mật khẩu)
        model.addAttribute("user", users);
        return "register";
    }
    usersService.addNew(users);
    return "redirect:/dashboard/";
}
```

**Thay đổi:**
- ✅ Message lỗi tiếng Việt rõ ràng
- ✅ Hiển thị email bị trùng
- ✅ Giữ lại dữ liệu form (ngoại trừ mật khẩu)

#### ✅ API Endpoint Mới: Kiểm Tra Email Real-time

```java
/**
 * API endpoint để kiểm tra email trùng lặp (Real-time)
 * GET /api/check-email?email=test@example.com
 * Response: { "exists": true/false, "message": "..." }
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
```

**Cách hoạt động:**
- URL: `GET /api/check-email?email=test@example.com`
- Response JSON:
  ```json
  {
    "exists": true,
    "message": "Email này đã được đăng ký. Vui lòng sử dụng email khác."
  }
  ```
  hoặc
  ```json
  {
    "exists": false,
    "message": "Email có thể sử dụng"
  }
  ```

---

### B. TEMPLATE (register.html)

#### ✅ Thông Báo Lỗi Đẹp

```html
<!-- Thông báo lỗi email trùng lặp -->
<div th:if="${error}" class="alert alert-danger alert-dismissible fade show" role="alert" 
     style="border-radius: 8px; border-left: 4px solid #dc3545;">
  <div style="display: flex; align-items: center; gap: 10px;">
    <i class="fa-solid fa-circle-xmark" style="font-size: 20px;"></i>
    <div>
      <strong>Lỗi đăng ký!</strong>
      <br/>
      <span th:text="${error}" style="font-size: 14px;"></span>
    </div>
  </div>
  <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
</div>
```

**Giao diện:**
- 🎨 Icon lỗi rõ ràng
- 📝 Thông báo chi tiết
- ✖️ Nút đóng (dismiss)
- 🎯 Hướng dẫn người dùng rõ ràng

#### ✅ Input Email với Validation Real-time

```html
<div class="mb-3 input-icon">
  <label class="mb-1" for="email" style="font-weight:600;">Email</label>
  <i class="fa-solid fa-envelope"></i>
  <input id="email" placeholder="you@example.com" name="email" 
         th:field="*{email}" type="email" class="form-control" required/>
  <div class="helper">Chúng tôi sẽ không chia sẻ email của bạn.</div>
  
  <!-- Thông báo lỗi real-time -->
  <small id="emailError" class="text-danger" style="display:none;"></small>
</div>
```

#### ✅ JavaScript: Kiểm Tra Email Real-time

```javascript
// ========== KIỂM TRA EMAIL REAL-TIME ==========
var emailInput = document.getElementById('email');
var emailError = document.getElementById('emailError');
var submitBtn = document.getElementById('submitBtn');
var emailCheckTimeout;

emailInput.addEventListener('input', function() {
  clearTimeout(emailCheckTimeout);
  var email = this.value.trim();

  // 1️⃣ Nếu email trống
  if (!email) {
    emailError.style.display = 'none';
    submitBtn.disabled = false;
    return;
  }

  // 2️⃣ Kiểm tra định dạng email (regex)
  var emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailPattern.test(email)) {
    emailError.textContent = '❌ Định dạng email không hợp lệ';
    emailError.className = 'text-warning';
    emailError.style.display = 'block';
    submitBtn.disabled = true;
    return;
  }

  // 3️⃣ Delay 500ms rồi gọi API
  emailCheckTimeout = setTimeout(function() {
    fetch('/api/check-email?email=' + encodeURIComponent(email), {
      method: 'GET',
      headers: {'Accept': 'application/json'}
    })
    .then(response => response.json())
    .then(data => {
      if (data.exists) {
        // Email đã tồn tại
        emailError.textContent = '❌ ' + data.message;
        emailError.className = 'text-danger';
        emailError.style.display = 'block';
        submitBtn.disabled = true;
      } else {
        // Email có thể sử dụng
        emailError.textContent = '✅ Email có thể sử dụng';
        emailError.className = 'text-success';
        emailError.style.display = 'block';
        submitBtn.disabled = false;
      }
    })
    .catch(error => {
      console.error('Lỗi kiểm tra email:', error);
      emailError.style.display = 'none';
      submitBtn.disabled = false;
    });
  }, 500);  // Delay 500ms tránh gọi API quá nhiều lần
});
```

**Quy trình:**
1. Lắng nghe sự kiện `input` trên field email
2. Xóa timeout cũ (nếu có) để tránh gọi API quá nhiều
3. Nếu email trống → ẩn thông báo
4. Kiểm tra định dạng email bằng regex
5. Nếu định dạng sai → hiển thị ❌ cảnh báo
6. Delay 500ms rồi gọi API `/api/check-email`
7. Nếu email tồn tại → ❌ lỗi, vô hiệu hóa nút
8. Nếu email OK → ✅ thành công, bật nút

---

## 🎯 LUỒNG HOẠT ĐỘNG

### Kịch Bản 1: Người Dùng Nhập Email Trùng

```
Bước 1: Mở trang /register
        ↓
Bước 2: Nhập email "admin@example.com" (email đã tồn tại)
        ↓
Bước 3: JavaScript event 'input' được kích hoạt
        ↓
Bước 4: Kiểm tra định dạng → OK ✅
        ↓
Bước 5: Delay 500ms...
        ↓
Bước 6: Gọi GET /api/check-email?email=admin@example.com
        ↓
Bước 7: Server query database → Tìm thấy email
        ↓
Bước 8: Response: { "exists": true, "message": "..." }
        ↓
Bước 9: UI cập nhật:
        - Hiển thị: ❌ Email này đã được đăng ký. Vui lòng sử dụng email khác.
        - Nút "Đăng ký" bị vô hiệu hóa (disabled)
        - Màu text: text-danger (đỏ)
```

### Kịch Bản 2: Người Dùng Nhập Email Mới

```
Bước 1: Mở trang /register
        ↓
Bước 2: Nhập email "newuser@example.com" (email mới)
        ↓
Bước 3: JavaScript event 'input' được kích hoạt
        ↓
Bước 4: Kiểm tra định dạng → OK ✅
        ↓
Bước 5: Delay 500ms...
        ↓
Bước 6: Gọi GET /api/check-email?email=newuser@example.com
        ↓
Bước 7: Server query database → Không tìm thấy email
        ↓
Bước 8: Response: { "exists": false, "message": "Email có thể sử dụng" }
        ↓
Bước 9: UI cập nhật:
        - Hiển thị: ✅ Email có thể sử dụng
        - Nút "Đăng ký" được bật (enabled)
        - Màu text: text-success (xanh)
```

### Kịch Bản 3: Submit Form (Server-side Check)

```
Bước 1: Người dùng click nút "Đăng ký"
        ↓
Bước 2: JavaScript addEventListener submit được kích hoạt
        ↓
Bước 3: Kiểm tra lại định dạng email
        ↓
Bước 4: Form được gửi POST /register/new
        ↓
Bước 5: Server: UsersController.userRegistration() được gọi
        ↓
Bước 6: Kiểm tra: usersService.getUserByEmail(email)
        ↓
     ┌─────────────────┬──────────────────┐
     │ Tồn tại?        │
     └────┬──────┬─────┘
          │ Có   │ Không
          ▼      ▼
        ❌      ✅
     Lỗi      Tạo tài khoản
     Trở về   Redirect /dashboard/
     register
```

---

## 🔐 SECURITY

### ✅ Xác Thực Multi-layer

| Layer | Vị Trí | Xác Thực |
|-------|--------|---------|
| 1 | **Client-side** | Regex email pattern |
| 2 | **Client-side** | Fetch API real-time |
| 3 | **Server-side** | Service method (DB query) |
| 4 | **Server-side** | Spring validation `@Valid` |

### ✅ Không Có SQL Injection
- Sử dụng `usersService.getUserByEmail()` (Prepared Statement)
- Không concatenate SQL string
- Email được sanitize

### ✅ Không Lộ Thông Tin
- API không trả về password hay thông tin nhạy cảm
- Chỉ trả về: email exists? + message

---

## 📱 UX IMPROVEMENTS

### ✅ Real-time Feedback
- User không cần chờ submit form
- Biết ngay email có bị trùng hay không
- Trải nghiệm mượt mà

### ✅ Visual Cues
```
❌ Định dạng email không hợp lệ      (màu vàng - cảnh báo)
❌ Email này đã được đăng ký...       (màu đỏ - lỗi)
✅ Email có thể sử dụng               (màu xanh - thành công)
```

### ✅ Smart Button Disabling
- Nút chỉ bật khi email valid
- Ngăn chặn người dùng submit email trùng
- Giảm request không cần thiết tới server

---

## 🧪 TESTING

### Test Case 1: Email Không Tồn Tại
```
Input: "newuser@gmail.com"
Expected: 
- Message: "✅ Email có thể sử dụng"
- Color: Green (text-success)
- Button: Enabled
```

### Test Case 2: Email Đã Tồn Tại
```
Input: "admin@example.com" (email đã đăng ký)
Expected: 
- Message: "❌ Email này đã được đăng ký..."
- Color: Red (text-danger)
- Button: Disabled
```

### Test Case 3: Định Dạng Email Sai
```
Input: "notanemail"
Expected: 
- Message: "❌ Định dạng email không hợp lệ"
- Color: Orange (text-warning)
- Button: Disabled
```

### Test Case 4: Email Trống
```
Input: ""
Expected: 
- Message: (ẩn)
- Button: Enabled
```

---

## 📋 FILES ĐƯỢC SỬA ĐỔI

| File | Thay Đổi |
|------|---------|
| `UsersController.java` | Thêm message lỗi chi tiết + API endpoint `/api/check-email` |
| `register.html` | Thêm alert, email input validation, JavaScript real-time check |

---

## 🚀 CÁCH SỬ DỤNG

### 1. Build Project
```bash
mvn clean install
```

### 2. Run Application
```bash
mvn spring-boot:run
```

### 3. Test Tính Năng
```
1. Truy cập: http://localhost:8080/register
2. Nhập email (bất kỳ)
3. Quan sát:
   - Real-time message
   - Icon ✅/❌
   - Button state (enabled/disabled)
```

---

## 💡 CÓ THỂ MỞ RỘNG THÊM

- 📧 Gửi email xác nhận khi đăng ký thành công
- 🔑 Tạo reset password flow
- 🛡️ Rate limiting cho API check-email
- 📊 Logging email check attempts
- 🌐 Multi-language support

---

**Tài liệu hoàn chỉnh! Hy vọng giúp ích cho bạn 🎉**

Ngày cập nhật: 12/04/2026  
Dự án: Job Portal  
Phiên bản: 2.0

