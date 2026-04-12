# ✅ HƯỚNG DẪN: Validation CV & Ảnh (Bắt Buộc)

---

## 🎯 TÍNH NĂNG

Hệ thống giờ yêu cầu:
- ✅ **CV (Resume)**: BẮT BUỘC - không được để trống
- ✅ **Ảnh đại diện**: BẮT BUỘC - không được để trống
- ✅ **Các trường khác**: TÙY CHỌN - nếu trống thì hiển thị trắng

---

## 📊 SO SÁNH TRƯỚC SAU

| Trường | TRƯỚC | SAU |
|--------|-------|-----|
| **CV** | Tùy chọn | ✅ **BẮT BUỘC** |
| **Ảnh** | Tùy chọn | ✅ **BẮT BUỘC** |
| **Tên, Địa chỉ, v.v** | Tùy chọn | Tùy chọn |
| **Error Message** | Không có | ✅ **CÓ (phía client + server)** |

---

## 🔧 CHI TIẾT IMPLEMENTATION

### 1️⃣ ENTITY - Thêm @NotBlank Annotations

**File:** `JobSeekerProfile.java`

```java
@NotBlank(message = "CV không được để trống. Vui lòng upload file CV.")
private String resume;

@Column(nullable = true, length = 64)
@NotBlank(message = "Ảnh đại diện không được để trống. Vui lòng chọn ảnh.")
private String profilePhoto;
```

**Tác dụng:**
- JPA validation annotations
- Kiểm tra trường không được null hoặc chỉ chứa khoảng trắng
- Message tự động được đưa vào error binding

---

### 2️⃣ CONTROLLER - Server-side Validation

**File:** `JobSeekerProfileController.java`

```java
@PostMapping("/addNew")
public String addNew(...) {
    // ...
    
    List<String> errors = new ArrayList<>();

    // ✅ VALIDATION: Kiểm tra CV (bắt buộc)
    if (pdf.isEmpty() || Objects.equals(pdf.getOriginalFilename(), "")) {
        errors.add("❌ CV không được để trống. Vui lòng upload file CV.");
    } else {
        resumeName = StringUtils.cleanPath(Objects.requireNonNull(pdf.getOriginalFilename()));
        jobSeekerProfile.setResume(resumeName);
    }

    // ✅ VALIDATION: Kiểm tra ảnh (bắt buộc)
    if (image.isEmpty() || Objects.equals(image.getOriginalFilename(), "")) {
        errors.add("❌ Ảnh đại diện không được để trống. Vui lòng chọn ảnh.");
    } else {
        imageName = StringUtils.cleanPath(Objects.requireNonNull(image.getOriginalFilename()));
        jobSeekerProfile.setProfilePhoto(imageName);
    }

    // 🚨 Nếu có lỗi → Trả về form với error messages
    if (!errors.isEmpty()) {
        model.addAttribute("errors", errors);
        model.addAttribute("profile", ...);
        return "job-seeker-profile";  // Quay lại form mà không lưu
    }
    
    // Nếu hợp lệ → Lưu và redirect
    jobSeekerProfileService.addNew(jobSeekerProfile);
    // ...
}
```

**Quy trình:**
1. Kiểm tra CV không trống
2. Kiểm tra ảnh không trống
3. Nếu cả hai đều hợp lệ → Lưu
4. Nếu có lỗi → Quay lại form + Hiển thị error

---

### 3️⃣ TEMPLATE - Display Error Messages

**File:** `job-seeker-profile.html`

```html
<!-- Alert hiển thị error messages -->
<div th:if="${errors != null && errors.size() > 0}" class="alert alert-danger alert-dismissible fade show" role="alert">
    <div style="display: flex; align-items: flex-start; gap: 15px;">
        <i class="fa-solid fa-circle-xmark" style="font-size: 20px;"></i>
        <div>
            <strong>⚠️ Vui lòng sửa lỗi sau:</strong>
            <ul style="margin-top: 10px;">
                <li th:each="error : ${errors}" th:text="${error}"></li>
            </ul>
        </div>
    </div>
    <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
</div>
```

**Giao diện:**
- 🔴 Red alert box
- 🚨 Danh sách lỗi rõ ràng
- ✖️ Nút đóng

---

### 4️⃣ TEMPLATE - Label "Bắt Buộc"

**CV:**
```html
<div class="add-row-title">
    CV
    <span style="color: red; font-weight: bold;"> *</span>
    <small style="color: #666;"> (Bắt buộc)</small>
</div>
```

**Ảnh:**
```html
<div class="add-row-title">
    Ảnh đại diện
    <span style="color: red; font-weight: bold;"> *</span>
    <small style="color: #666;"> (Bắt buộc)</small>
</div>
```

**Giao diện:**
- 🔴 Dấu sao đỏ (*)
- 📝 Text "(Bắt buộc)"

---

### 5️⃣ TEMPLATE - Client-side Validation (JavaScript)

**File:** `job-seeker-profile.html`

```javascript
form.addEventListener("submit", function (e) {
    const cvFile = cvInput && cvInput.files && cvInput.files[0];
    const imageFile = imageInput && imageInput.files && imageInput.files[0];
    const errors = [];

    // Kiểm tra CV
    if (!cvFile && (!resumeName || resumeName.textContent === "Chưa có CV")) {
        errors.push("❌ CV không được để trống. Vui lòng upload file CV.");
    }

    // Kiểm tra ảnh
    if (!imageFile && form.getAttribute("data-has-image") !== "true") {
        errors.push("❌ Ảnh đại diện không được để trống. Vui lòng chọn ảnh.");
    }

    // Nếu có lỗi → Hiển thị alert & ngăn submit
    if (errors.length > 0) {
        e.preventDefault();
        alert(errors.join("\n"));
        return false;
    }
});
```

**Quy trình:**
1. Khi user click "Lưu"
2. Kiểm tra CV file hoặc resume name
3. Kiểm tra ảnh file hoặc data-has-image
4. Nếu có lỗi → Alert + Ngăn submit form
5. Nếu OK → Form được gửi tới server

---

## 🎯 LUỒNG HOẠT ĐỘNG

### Kịch Bản 1: CV & Ảnh OK

```
1. User vào trang job-seeker-profile/
   ├─ Điền thông tin (hoặc bỏ trống trường tùy chọn)
   ├─ Upload CV
   └─ Chọn ảnh
   
2. Click "Lưu"
   ├─ JavaScript kiểm tra:
   │  ├─ CV file ✅
   │  └─ Ảnh file ✅
   └─ Form được submit
   
3. Server kiểm tra lại:
   ├─ CV file ✅
   ├─ Ảnh file ✅
   └─ Lưu vào database
   
4. Redirect → /dashboard/
```

### Kịch Bản 2: CV Trống

```
1. User vào trang job-seeker-profile/
   ├─ Chọn ảnh ✅
   └─ Không upload CV ❌
   
2. Click "Lưu"
   ├─ JavaScript kiểm tra:
   │  ├─ CV file ❌
   │  └─ Alert: "CV không được để trống..."
   └─ Submit bị ngăn
```

### Kịch Bản 3: Ảnh Trống

```
1. User vào trang job-seeker-profile/
   ├─ Upload CV ✅
   └─ Không chọn ảnh ❌
   
2. Click "Lưu"
   ├─ JavaScript kiểm tra:
   │  ├─ Ảnh file ❌
   │  └─ Alert: "Ảnh đại diện không được để trống..."
   └─ Submit bị ngăn
```

### Kịch Bản 4: Cả hai trống

```
1. User vào trang job-seeker-profile/
   ├─ Không upload CV ❌
   └─ Không chọn ảnh ❌
   
2. Click "Lưu"
   ├─ JavaScript kiểm tra:
   │  ├─ Cả CV và ảnh đều lỗi
   │  └─ Alert hiển thị cả 2 lỗi
   └─ Submit bị ngăn
   
3. User sửa lỗi → Click "Lưu" lại → OK
```

---

## 📊 VALIDATION LAYERS (2 TẦNG)

### Layer 1: Client-side (JavaScript)
```
User submit → Kiểm tra ngay lập tức → 
Alert error → Form không được gửi
```

**Ưu điểm:**
- ✅ Nhanh
- ✅ Instant feedback
- ✅ Giảm request tới server

---

### Layer 2: Server-side (Java Controller)
```
Form được gửi → Server kiểm tra → 
Nếu lỗi: Quay lại form + Hiển thị error list →
Người dùng sửa lỗi → Gửi lại

Nếu OK: Lưu database → Redirect
```

**Ưu điểm:**
- ✅ Bảo mật (user không thể bypass JavaScript)
- ✅ Kiểm tra lại file thực tế
- ✅ Hiển thị error alert chuyên nghiệp

---

## 🎨 UI/UX

### Alert Error Messages

```
🔴 ERROR BOX:
┌─────────────────────────────────────┐
│ ❌ ⚠️ Vui lòng sửa lỗi sau:           │
│    • ❌ CV không được để trống...    │
│    • ❌ Ảnh đại diện không được...  │
│                                ✖️   │
└─────────────────────────────────────┘
```

**Đặc điểm:**
- Icon rõ ràng (❌, ⚠️)
- Text tiếng Việt dễ hiểu
- Danh sách đầy đủ lỗi
- Nút đóng (dismiss)

---

### Labels "Bắt Buộc"

```
CV *
(Bắt buộc)

Ảnh đại diện *
(Bắt buộc)
```

**Đặc điểm:**
- 🔴 Dấu sao đỏ
- 📝 Text "(Bắt buộc)"
- Rõ ràng cho người dùng

---

## 🧪 TESTING

### Test 1: Upload CV & Ảnh Thành Công

```
Step 1: Vào /job-seeker-profile/
Step 2: Upload CV (PDF)
Step 3: Chọn ảnh (PNG/JPG)
Step 4: Click "Lưu"

Expected:
✅ Lưu thành công
✅ Redirect tới /dashboard/
```

---

### Test 2: Không Upload CV

```
Step 1: Vào /job-seeker-profile/
Step 2: Chỉ chọn ảnh
Step 3: Không upload CV
Step 4: Click "Lưu"

Expected:
❌ Alert: "CV không được để trống..."
❌ Form không submit
```

---

### Test 3: Không Chọn Ảnh

```
Step 1: Vào /job-seeker-profile/
Step 2: Chỉ upload CV
Step 3: Không chọn ảnh
Step 4: Click "Lưu"

Expected:
❌ Alert: "Ảnh đại diện không được để trống..."
❌ Form không submit
```

---

### Test 4: Cả hai trống

```
Step 1: Vào /job-seeker-profile/
Step 2: Không upload CV
Step 3: Không chọn ảnh
Step 4: Click "Lưu"

Expected:
❌ Alert hiển thị cả 2 lỗi
❌ Form không submit
```

---

### Test 5: Trường Tùy Chọn Để Trống

```
Step 1: Upload CV ✅
Step 2: Chọn ảnh ✅
Step 3: Không điền tên (firstName, lastName)
Step 4: Click "Lưu"

Expected:
✅ Lưu thành công (trường tên có thể trống)
✅ Redirect tới /dashboard/
✅ Tên hiển thị trắng khi xem hồ sơ
```

---

## 📁 FILES ĐÃ SỬA ĐỔI

| File | Thay Đổi |
|------|---------|
| **JobSeekerProfile.java** | ✏️ Thêm @NotBlank annotations |
| **JobSeekerProfileController.java** | ✏️ Thêm validation logic + error messages |
| **job-seeker-profile.html** | ✏️ Thêm alert box, labels "Bắt buộc", JavaScript validation |

---

## 🔐 SECURITY

✅ **Bảo vệ bằng:**
- Server-side validation (không thể bypass)
- File type checking (PDF for CV, PNG/JPG for image)
- File size limits
- User authentication required

---

## 📝 VALIDATION MESSAGES

### Server Error Messages (Tiếng Việt)

```
❌ CV không được để trống. Vui lòng upload file CV.
❌ Ảnh đại diện không được để trống. Vui lòng chọn ảnh.
```

### Client Alert Messages (Tiếng Việt)

```
❌ CV không được để trống. Vui lòng upload file CV.
❌ Ảnh đại diện không được để trống. Vui lòng chọn ảnh.
```

---

## 🚀 DEPLOYMENT

### Bước 1: Build
```bash
mvn clean install
```

### Bước 2: Run
```bash
mvn spring-boot:run
```

### Bước 3: Test
```
http://localhost:8080/job-seeker-profile/
```

---

## 💡 TIPS

### Nếu muốn thay đổi validation

**1. Trường CV (optional → required):**
Đã xong ✅

**2. Trường Ảnh (optional → required):**
Đã xong ✅

**3. Trường khác (tên, địa chỉ) thành required:**
- Thêm `@NotBlank` annotation vào Entity
- Thêm validation check trong Controller
- Update error messages

### Nếu muốn thay đổi file type

**CV:**
```html
<input type="file" accept="application/pdf" ... />
```

**Ảnh:**
```html
<input type="file" accept="image/png, image/jpeg" ... />
```

---

## ✨ SUMMARY

| Khía Cạnh | Chi Tiết |
|----------|---------|
| **CV** | BẮT BUỘC ✅ |
| **Ảnh** | BẮT BUỘC ✅ |
| **Validation** | 2 layers (Client + Server) ✅ |
| **Error Message** | Tiếng Việt rõ ràng ✅ |
| **User Experience** | Alert + Form validation ✅ |
| **Security** | Server-side check ✅ |

---

**🎉 Validation CV & Ảnh hoàn chỉnh!**

Ngày: 12/04/2026  
Dự án: Job Portal v3.1

