# 🎯 Hướng Dẫn: Chức Năng Hủy Ứng Tuyển & Bỏ Lưu Công Việc

---

## ✨ Tính Năng Được Thêm

Hệ thống giờ có **chức năng hủy ứng tuyển** và **bỏ lưu công việc**:

### 📋 Trước Đây (Chỉ đọc):
```
Nút "Đã ứng tuyển" → DISABLED (không thể click)
Nút "Đã lưu" → DISABLED (không thể click)
```

### ✅ Sau Khi Cập Nhật (Có chức năng):
```
Nút "Đã ứng tuyển" → CÓ THỂ CLICK để hủy ứng tuyển
Nút "Đã lưu" → CÓ THỂ CLICK để bỏ lưu
Nút "Ứng tuyển" → CÓ THỂ CLICK để ứng tuyển
Nút "Lưu" → CÓ THỂ CLICK để lưu
```

---

## 🗂️ CẤU TRÚC HOẠT ĐỘNG

### Trạng Thái Nút "Ứng Tuyển"

```
┌─────────────────────────────────────────┐
│  Người Dùng Xem Chi Tiết Job            │
└────────────────┬────────────────────────┘
                 │
        ┌────────▼────────┐
        │ Đã ứng tuyển?   │
        └────┬──────┬────┘
             │ Không │ Có
             ▼      ▼
          ┌──┐   ┌──────────────────┐
          │  │   │ Form /unapply    │
       ┌──┴──┴───┤ POST Method      │
       │  Form   │ Nút: Hủy ứng     │
       │  /apply │ tuyển (+confirm) │
       │  POST   └──────────────────┘
       │  Method │
       │  Nút:   │
       │  Ứng    │
       │  tuyển  │
       └─────────┘
```

### Trạng Thái Nút "Lưu"

```
┌──────────────────────────────────────┐
│  Người Dùng Xem Chi Tiết Job         │
└────────────────┬─────────────────────┘
                 │
        ┌────────▼────────┐
        │ Đã lưu?         │
        └────┬──────┬────┘
             │ Không │ Có
             ▼      ▼
          ┌──┐   ┌──────────────────┐
          │  │   │ Form /unsave     │
       ┌──┴──┴───┤ POST Method      │
       │  Form   │ Nút: Bỏ lưu      │
       │  /save  │ (+confirm)       │
       │  POST   └──────────────────┘
       │  Method │
       │  Nút:   │
       │  Lưu    │
       └─────────┘
```

---

## 🔧 CHI TIẾT IMPLEMENTATIONS

### A. BACKEND (Đã Tồn Tại Trước)

Controller đã có 2 endpoint để xử lý:

#### 1️⃣ Hủy Ứng Tuyển - JobSeekerApplyController.java

```java
@PostMapping("job-details/unapply/{id}")
public String unapply(@PathVariable("id") int id) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof AnonymousAuthenticationToken)) {
        String currentUsername = authentication.getName();
        Users user = usersService.findByEmail(currentUsername);
        Optional<JobSeekerProfile> seekerProfile = jobSeekerProfileService.getOne(user.getUserId());
        JobPostActivity jobPostActivity = jobPostActivityService.getOne(id);

        if (seekerProfile.isPresent() && jobPostActivity != null) {
            JobSeekerProfile profile = seekerProfile.get();
            jobSeekerApplyService.unapplyJob(profile, jobPostActivity);
        } else {
            throw new RuntimeException("User not found");
        }
    }
    return "redirect:/job-details-apply/" + id;
}
```

**Quy trình:**
1. Lấy người dùng đang đăng nhập
2. Lấy profile Job Seeker của người dùng
3. Lấy thông tin job
4. Gọi service để xóa bản ghi từ bảng `job_seeker_apply`
5. Redirect lại trang job details

#### 2️⃣ Bỏ Lưu - JobSeekerSaveController.java

```java
@PostMapping("job-details/unsave/{id}")
public String unsave(@PathVariable("id") int id) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof AnonymousAuthenticationToken)) {
        String currentUsername = authentication.getName();
        Users user = usersService.findByEmail(currentUsername);
        Optional<JobSeekerProfile> seekerProfile = jobSeekerProfileService.getOne(user.getUserId());
        JobPostActivity jobPostActivity = jobPostActivityService.getOne(id);

        if (seekerProfile.isPresent() && jobPostActivity != null) {
            JobSeekerProfile profile = seekerProfile.get();
            jobSeekerSaveService.unsaveJob(profile, jobPostActivity);
        } else {
            throw new RuntimeException("User not found");
        }
    }
    return "redirect:/job-details-apply/" + id;
}
```

**Quy trình:**
1. Lấy người dùng đang đăng nhập
2. Lấy profile Job Seeker của người dùng
3. Lấy thông tin job
4. Gọi service để xóa bản ghi từ bảng `job_seeker_save`
5. Redirect lại trang job details

---

### B. FRONTEND (Cập Nhật)

#### 1️⃣ Trang Job Details (job-details.html)

**Trước:** Nút disabled (không click được)
```html
<button class="apply-btn" type="submit" th:disabled="${alreadyApplied}"
        th:text="${alreadyApplied} ? 'Đã ứng tuyển' : 'Ứng tuyển'">
</button>
```

**Sau:** 2 form riêng biệt

```html
<!-- Nếu chưa ứng tuyển: Nút Ứng tuyển -->
<form th:if="${!alreadyApplied}"
      th:action="@{/job-details/apply/{id}(id=${jobDetails.jobPostId})}"
      method="post" sec:authorize="hasAuthority('Job Seeker')">
    <th:block th:if="${_csrf != null}">
        <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
    </th:block>
    <button class="apply-btn" type="submit">
        <i class="fa-regular fa-paper-plane"></i> Ứng tuyển
    </button>
</form>

<!-- Nếu đã ứng tuyển: Nút Hủy ứng tuyển -->
<form th:if="${alreadyApplied}"
      th:action="@{/job-details/unapply/{id}(id=${jobDetails.jobPostId})}"
      method="post" sec:authorize="hasAuthority('Job Seeker')">
    <th:block th:if="${_csrf != null}">
        <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
    </th:block>
    <button class="apply-btn applied" type="submit"
            onclick="return confirm('Bạn có chắc muốn hủy ứng tuyển không?')">
        <i class="fa-solid fa-check"></i> Đã ứng tuyển
    </button>
</form>
```

**Tương tự cho nút Lưu/Bỏ lưu:**

```html
<!-- Nếu chưa lưu: Nút Lưu -->
<form th:if="${!alreadySaved}"
      th:action="@{/job-details/save/{id}(id=${jobDetails.jobPostId})}"
      method="post" sec:authorize="hasAuthority('Job Seeker')">
    <!-- ... -->
    <button class="save-btn" type="submit">
        <i class="fa-regular fa-bookmark"></i> Lưu
    </button>
</form>

<!-- Nếu đã lưu: Nút Bỏ lưu -->
<form th:if="${alreadySaved}"
      th:action="@{/job-details/unsave/{id}(id=${jobDetails.jobPostId})}"
      method="post" sec:authorize="hasAuthority('Job Seeker')">
    <!-- ... -->
    <button class="save-btn saved" type="submit"
            onclick="return confirm('Bạn có chắc muốn bỏ lưu công việc này không?')">
        <i class="fa-solid fa-bookmark"></i> Đã lưu
    </button>
</form>
```

#### 2️⃣ Trang Applied Jobs (applied-jobs.html)

**Trước:** Chỉ hiển thị text
```html
<div class="applyjob" th:classappend="${jobPost.isActive} ? ' applied' : ' not-applied'">
    <i class="fa-regular fa-paper-plane"></i>
    <span th:text="${jobPost.isActive} ? 'Đã ứng tuyển' : 'Ứng tuyển'"></span>
</div>
```

**Sau:** Form có thể click để hủy

```html
<div class="applyjob" th:classappend="${jobPost.isActive} ? ' applied' : ' not-applied'">
    <form th:action="@{/job-details/unapply/{id}(id=${jobPost.jobPostId})}"
          method="post" style="display: inline;">
        <th:block th:if="${_csrf != null}">
            <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
        </th:block>
        <button type="submit" style="background: none; border: none; cursor: pointer; color: inherit;"
                onclick="return confirm('Bạn có chắc muốn hủy ứng tuyển không?')">
            <i class="fa-regular fa-paper-plane"></i>
            <span>Hủy ứng tuyển</span>
        </button>
    </form>
</div>
```

**Tương tự cho nút Lưu/Bỏ lưu:**

```html
<div class="savejob" th:classappend="${jobPost.isSaved} ? ' saved' : ' not-saved'">
    <form th:if="${jobPost.isSaved}" 
          th:action="@{/job-details/unsave/{id}(id=${jobPost.jobPostId})}"
          method="post" style="display: inline;">
        <th:block th:if="${_csrf != null}">
            <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
        </th:block>
        <button type="submit" style="background: none; border: none; cursor: pointer; color: inherit;"
                onclick="return confirm('Bạn có chắc muốn bỏ lưu công việc này không?')">
            <i class="fa-solid fa-bookmark"></i>
            <span>Đã Lưu</span>
        </button>
    </form>
    <form th:if="${!jobPost.isSaved}" 
          th:action="@{/job-details/save/{id}(id=${jobPost.jobPostId})}"
          method="post" style="display: inline;">
        <th:block th:if="${_csrf != null}">
            <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
        </th:block>
        <button type="submit" style="background: none; border: none; cursor: pointer; color: inherit;">
            <i class="fa-regular fa-bookmark"></i>
            <span>Lưu</span>
        </button>
    </form>
</div>
```

#### 3️⃣ Trang Saved Jobs (saved-jobs.html)

**Tương tự như Applied Jobs:**
- Nút bỏ lưu (luôn hiển thị vì đang ở trang saved jobs)
- Nút ứng tuyển/hủy ứng tuyển (tùy theo đã ứng tuyển hay chưa)

---

## 🎯 LUỒNG HOẠT ĐỘNG

### Kịch Bản 1: Hủy Ứng Tuyển

```
1. Người dùng xem chi tiết job
   ├─ Đã ứng tuyển trước đó
   └─ Nút hiển thị: "✓ Đã ứng tuyển"
   
2. Click nút "✓ Đã ứng tuyển"
   ↓
3. Confirm dialog: "Bạn có chắc muốn hủy ứng tuyển không?"
   ├─ Click "OK" → tiếp tục
   └─ Click "Cancel" → hủy
   
4. POST /job-details/unapply/{jobId}
   ↓
5. Server:
   ├─ Lấy user đang đăng nhập
   ├─ Lấy JobSeekerProfile
   ├─ Gọi jobSeekerApplyService.unapplyJob()
   │  └─ DELETE từ bảng job_seeker_apply
   └─ Redirect tới /job-details-apply/{jobId}
   
6. Page refresh:
   ├─ alreadyApplied = false
   └─ Nút thay đổi thành "Ứng tuyển"
```

### Kịch Bản 2: Bỏ Lưu Công Việc

```
1. Người dùng ở trang "Công việc đã lưu"
   ├─ Đã lưu công việc
   └─ Nút hiển thị: "Bỏ lưu"
   
2. Click nút "Bỏ lưu"
   ↓
3. Confirm dialog: "Bạn có chắc muốn bỏ lưu công việc này không?"
   ├─ Click "OK" → tiếp tục
   └─ Click "Cancel" → hủy
   
4. POST /job-details/unsave/{jobId}
   ↓
5. Server:
   ├─ Lấy user đang đăng nhập
   ├─ Lấy JobSeekerProfile
   ├─ Gọi jobSeekerSaveService.unsaveJob()
   │  └─ DELETE từ bảng job_seeker_save
   └─ Redirect tới /job-details-apply/{jobId}
   
6. Page refresh:
   ├─ alreadySaved = false
   └─ Nút thay đổi thành "Lưu"
```

### Kịch Bản 3: Ứng Tuyển Từ Trang Saved Jobs

```
1. Người dùng ở trang "Công việc đã lưu"
   ├─ Chưa ứng tuyển
   └─ Nút "Ứng tuyển" có thể click
   
2. Click nút "Ứng tuyển"
   ↓
3. POST /job-details/apply/{jobId}
   ↓
4. Server:
   ├─ Lấy user đang đăng nhập
   ├─ Tạo JobSeekerApply mới
   ├─ Lưu vào bảng job_seeker_apply
   └─ Redirect tới /job-details-apply/{jobId}
   
5. Page refresh:
   ├─ alreadyApplied = true
   └─ Nút thay đổi thành "Hủy ứng tuyển"
```

---

## 🔐 SECURITY

### ✅ Bảo Vệ CSRF
```html
<th:block th:if="${_csrf != null}">
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
</th:block>
```

### ✅ Xác Thực Người Dùng
```java
if (!(authentication instanceof AnonymousAuthenticationToken)) {
    // Chỉ người dùng đã đăng nhập mới có thể hủy
}
```

### ✅ Confirm Dialog
```javascript
onclick="return confirm('Bạn có chắc muốn hủy ứng tuyển không?')"
```

---

## 📊 CẤP CẬP TRÊN CÁC TRANG

| Trang | Thay Đổi |
|------|---------|
| **job-details.html** | ✅ Nút ứng tuyển/hủy + Nút lưu/bỏ lưu |
| **applied-jobs.html** | ✅ Nút hủy ứng tuyển + Nút lưu/bỏ lưu |
| **saved-jobs.html** | ✅ Nút bỏ lưu + Nút ứng tuyển/hủy |

---

## 📁 FILES ĐÃ CẬP NHẬT

| File | Thay Đổi |
|------|---------|
| `job-details.html` | Thêm form hủy ứng tuyển & bỏ lưu |
| `applied-jobs.html` | Thêm form hủy & lưu/bỏ lưu |
| `saved-jobs.html` | Thêm form ứng tuyển/hủy & bỏ lưu |

---

## ✨ USER EXPERIENCE

### Trước
```
❌ Nút disabled (xám, không click được)
❌ Người dùng bất lực
❌ Phải refresh trang để thay đổi trạng thái
```

### Sau
```
✅ Nút có thể click
✅ Hiển thị confirm dialog
✅ Action ngay lập tức
✅ Trang tự refresh
✅ Icon & text rõ ràng
```

---

## 🧪 TESTING

### Test Case 1: Hủy Ứng Tuyển
```
1. Đăng nhập
2. Vào chi tiết job
3. Click "✓ Đã ứng tuyển"
4. Confirm dialog
5. Click OK
6. Verify:
   - Nút thay đổi thành "Ứng tuyển"
   - icon đổi từ checkmark
```

### Test Case 2: Bỏ Lưu
```
1. Đăng nhập
2. Vào trang "Công việc đã lưu"
3. Click "Bỏ lưu"
4. Confirm dialog
5. Click OK
6. Verify:
   - Nút thay đổi thành "Lưu"
   - icon đổi từ solid bookmark
```

### Test Case 3: Ứng Tuyển Lại
```
1. Hủy ứng tuyển trước
2. Click "Ứng tuyển"
3. Verify:
   - Nút thay đổi thành "Hủy ứng tuyển"
   - icon đổi thành checkmark
```

---

## 🚀 CÓ THỂ MỞ RỘNG THÊM

- 🔔 Hiển thị toast notification sau khi hủy
- 📊 Thêm animation khi nút thay đổi
- ⚡ AJAX để update mà không cần reload trang
- 📧 Gửi email xác nhận khi hủy ứng tuyển

---

**Hệ thống giờ đã hoàn chỉnh hơn với chức năng hủy & bỏ lưu! 🎉**

Ngày cập nhật: 12/04/2026  
Dự án: Job Portal  
Phiên bản: 3.0

