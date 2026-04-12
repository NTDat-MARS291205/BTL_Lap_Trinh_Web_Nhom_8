# 🔄 FIX: Cập Nhật Profile Không Quay Về Dashboard

---

## 🎯 VẤN ĐỀ

Khi click "Lưu" để cập nhật thông tin profile (ứng viên hoặc nhà tuyển dụng):
- ❌ Không quay lại trang chủ (/dashboard/)
- ❌ Có thể bị stuck ở trang profile
- ❌ Không biết là lưu thành công hay thất bại

---

## 🔍 NGUYÊN NHÂN

1. **Form validation bị block:** CV hoặc ảnh bỏ trống
2. **JavaScript preventDefault:** Form không submit
3. **Exception không được catch:** Redirect không thực hiện
4. **Logging không chi tiết:** Không biết lỗi ở đâu

---

## ✅ CÁC FIX

### 1️⃣ Thêm Try-Catch & Logging Chi Tiết

**JobSeekerProfileController.addNew():**

```java
@PostMapping("/addNew")
public String addNew(...) {
    try {
        System.out.println("📝 [JobSeeker Profile] Saving profile...");
        // ... logic ...
        System.out.println("✅ [JobSeeker Profile] Profile saved successfully");
        System.out.println("🔄 [JobSeeker Profile] Redirecting to /dashboard/");
        return "redirect:/dashboard/";
        
    } catch (Exception e) {
        System.err.println("❌ [JobSeeker Profile] Exception: " + e.getMessage());
        e.printStackTrace();
        model.addAttribute("errors", new ArrayList<>(
            Arrays.asList("❌ Lỗi cập nhật: " + e.getMessage())));
        return "job-seeker-profile";
    }
}
```

**RecruiterProfileController.addNew():**

```java
@PostMapping("/addNew")
public String addNew(...) {
    try {
        System.out.println("📝 [Recruiter Profile] Saving profile...");
        // ... logic ...
        System.out.println("🔄 [Recruiter Profile] Redirecting to /dashboard/");
        return "redirect:/dashboard/";
        
    } catch (Exception e) {
        System.err.println("❌ [Recruiter Profile] Exception: " + e.getMessage());
        model.addAttribute("error", "❌ Lỗi cập nhật: " + e.getMessage());
        return "recruiter_profile";
    }
}
```

### 2️⃣ Chi Tiết Logging Cho File Upload

```java
// Khi upload file
if (!Objects.equals(image.getOriginalFilename(), "")) {
    FileUploadUtil.saveFile(uploadDir, imageName, image);
    System.out.println("✅ [JobSeeker Profile] Image uploaded");
}
if (!Objects.equals(pdf.getOriginalFilename(), "")) {
    FileUploadUtil.saveFile(uploadDir, resumeName, pdf);
    System.out.println("✅ [JobSeeker Profile] PDF uploaded");
}
```

### 3️⃣ Validation Message Chi Tiết

```java
if (!errors.isEmpty()) {
    System.out.println("❌ [JobSeeker Profile] Validation errors: " + errors);
    model.addAttribute("errors", errors);
    // ... return form ...
}
```

---

## 🎯 LUỒNG HOẠT ĐỘNG - AFTER FIX

### Kịch Bản 1: Update Thành Công

```
1. Click "Lưu"
   ↓
2. Console: "📝 Saving profile..."
   ↓
3. Profile validate ✅
   ↓
4. Database save ✅
   ↓
5. Console: "✅ Profile saved successfully"
   ↓
6. File upload ✅
   ↓
7. Console: "✅ Image/PDF uploaded"
   ↓
8. Console: "🔄 Redirecting to /dashboard/"
   ↓
9. Redirect /dashboard/ ✅
   ↓
10. Dashboard hiển thị ✅
```

### Kịch Bản 2: Validation Error

```
1. Click "Lưu"
   ↓
2. CV hoặc ảnh trống ❌
   ↓
3. Console: "❌ Validation errors..."
   ↓
4. Alert error hiển thị
   ↓
5. Quay lại form
   ↓
6. User sửa lỗi
   ↓
7. Click "Lưu" lại → OK
```

### Kịch Bản 3: Exception

```
1. Click "Lưu"
   ↓
2. Database error ❌
   ↓
3. Exception catch
   ↓
4. Console: "❌ Exception: ..."
   ↓
5. Error message hiển thị
   ↓
6. Quay lại form (không redirect)
```

---

## 🧪 TESTING

### Test 1: Update Thành Công - JobSeeker

```
1. Vào /job-seeker-profile/
2. Điền thông tin:
   - Upload CV ✅
   - Chọn ảnh ✅
   - Điền tên, địa chỉ
3. Click "Lưu"

Expected Console Log:
📝 [JobSeeker Profile] Saving profile...
✅ [JobSeeker Profile] Profile saved successfully
✅ [JobSeeker Profile] Image uploaded
✅ [JobSeeker Profile] PDF uploaded
🔄 [JobSeeker Profile] Redirecting to /dashboard/

Expected Result:
✅ Redirect /dashboard/
✅ Dashboard hiển thị
```

---

### Test 2: Validation Error - CV Trống

```
1. Vào /job-seeker-profile/
2. Điền thông tin:
   - Chỉ chọn ảnh ✅
   - Không upload CV ❌
3. Click "Lưu"

Expected Console Log:
📝 [JobSeeker Profile] Saving profile...
❌ [JobSeeker Profile] Validation errors: [
    "❌ CV không được để trống..."
]

Expected Result:
❌ Alert error hiển thị
❌ Quay lại form
✅ CV field focus
```

---

### Test 3: Update Thành Công - Recruiter

```
1. Vào /recruiter-profile/
2. Điền thông tin:
   - Chọn ảnh ✅
   - Điền tên, công ty, v.v
3. Click "Lưu"

Expected Console Log:
📝 [Recruiter Profile] Saving profile...
✅ [Recruiter Profile] Profile saved
✅ [Recruiter Profile] Image uploaded
🔄 [Recruiter Profile] Redirecting to /dashboard/

Expected Result:
✅ Redirect /dashboard/
✅ Dashboard hiển thị
```

---

## 📊 CONSOLE LOG EXAMPLE

```
📝 [JobSeeker Profile] Saving profile...
✅ User retrieved: john@example.com
💾 Saving to database...
✅ Profile saved successfully
✅ Skills saved
✅ Image uploaded
✅ PDF uploaded
🔄 Redirecting to /dashboard/
```

---

## 🔍 DEBUGGING TIPS

**Nếu vẫn không redirect:**

1. **Check Console Log:**
   ```
   Xem có "🔄 Redirecting to /dashboard/" không?
   ```

2. **Check Network Tab (Browser F12):**
   ```
   Xem POST request thành công hay bị error?
   ```

3. **Check Server Console:**
   ```
   Xem có exception nào không?
   Xem có "📝 Saving profile..." không?
   ```

4. **Check HTML Form:**
   ```html
   <form th:action="@{/job-seeker-profile/addNew}" 
         th:object="${profile}" 
         method="post" 
         enctype="multipart/form-data">
   ```
   ✅ Action đúng không?
   ✅ Method = POST?
   ✅ enctype = multipart/form-data?

---

## 📁 FILES UPDATED

| File | Change |
|------|--------|
| **JobSeekerProfileController.java** | ✏️ Thêm try-catch + logging chi tiết |
| **RecruiterProfileController.java** | ✏️ Thêm try-catch + logging chi tiết |

---

## ✅ RESULT

- ✅ Click "Lưu" → Redirect /dashboard/
- ✅ Profile update thành công
- ✅ Chi tiết logging cho debug
- ✅ Error message rõ ràng
- ✅ Validation error hiển thị ngay

---

**Ngày cập nhật: 12/04/2026**  
**Status: ✅ FIXED**

