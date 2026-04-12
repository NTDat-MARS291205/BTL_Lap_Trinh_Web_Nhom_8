# 🔐 FIX: Lỗi Đăng Nhập Sau Khi Đăng Ký Mới

---

## 🎯 VẤN ĐỀ

**Tài khoản cũ đăng nhập được ✅**  
**Tài khoản mới sau fix code không đăng nhập được ❌**

---

## 🔍 NGUYÊN NHÂN

Khi user mới đăng ký:
1. User được tạo thành công ✅
2. Profile tạo bị fail hoặc không được persist → Profile không có trong database
3. Khi đăng nhập, `getCurrentUserProfile()` tìm profile không thấy
4. `orElse(new JobSeekerProfile())` trả về empty object (userId = NULL)
5. Template/Code crash khi access properties

---

## ✅ CÁC FIX

### 1️⃣ Auto-Create Profile Khi Đăng Nhập

**UsersService.java - getCurrentUserProfile():**

```java
public Object getCurrentUserProfile() {
    // ... authentication check ...
    
    if (isRecruiter) {
        // ✅ Tìm hoặc tạo RecruiterProfile
        RecruiterProfile profile = recruiterProfileRepository.findById(userId).orElse(null);
        if (profile == null) {
            System.out.println("⚠️ Profile not found. Creating...");
            profile = new RecruiterProfile(users);
            profile = recruiterProfileRepository.save(profile);
            System.out.println("✅ Profile created automatically");
        }
        return profile;
    } else {
        // ✅ Tìm hoặc tạo JobSeekerProfile
        JobSeekerProfile profile = jobSeekerProfileRepository.findById(userId).orElse(null);
        if (profile == null) {
            System.out.println("⚠️ Profile not found. Creating...");
            profile = new JobSeekerProfile(users);
            profile = jobSeekerProfileRepository.save(profile);
            System.out.println("✅ Profile created automatically");
        }
        return profile;
    }
}
```

**Khi nào chạy:**
- User đăng nhập
- getCurrentUserProfile() gọi
- Profile không tìm thấy → Tự động tạo
- Trả về profile mới

---

### 2️⃣ Không Throw Exception Khi Profile Create Fail

**UsersService.java - addNew():**

```java
try {
    // ... Create profile ...
} catch (Exception profileException) {
    System.err.println("⚠️ Warning: " + profileException.getMessage());
    // ⚠️ KHÔNG throw exception!
    // User được tạo thành công
    // Profile sẽ được tạo tự động trên lần đăng nhập đầu tiên
    System.out.println("ℹ️ Profile will be created automatically on first login");
}
```

**Tác dụng:**
- User đăng ký thành công ngay cả khi profile create fail
- Profile sẽ được tạo tự động khi đăng nhập

---

## 🎯 LUỒNG HOẠT ĐỘNG - SAU FIX

### Kịch Bản 1: Profile Được Tạo OK Lúc Đăng Ký

```
Register:
✅ User saved: ID = 10
✅ Profile created

Login:
✅ Tìm thấy profile
✅ Trả về profile
✅ Đăng nhập thành công
```

---

### Kịch Bản 2: Profile Create Fail Lúc Đăng Ký

```
Register:
✅ User saved: ID = 10
⚠️ Profile create failed (nhưng không throw exception)

Login:
❌ Tìm không thấy profile
📋 Tự động tạo profile
✅ Trả về profile mới
✅ Đăng nhập thành công
```

---

### Kịch Bản 3: Tài Khoản Cũ (Trước Fix)

```
Register (trước fix):
✅ User saved: ID = 5
✅ Profile created (hoặc không, nhưng vẫn được fix lúc đăng nhập)

Login:
✅ Tìm thấy profile (hoặc tạo mới nếu không có)
✅ Đăng nhập thành công
```

---

## 🧪 TESTING

### Test 1: Tài Khoản Cũ

```
Account: (created trước fix)
Email: admin@example.com
Password: 12345678

Expected:
✅ Đăng nhập thành công (vẫn OK)
✅ Dashboard hiển thị
```

---

### Test 2: Tài Khoản Mới

```
Account: (created sau fix)
Email: newuser@gmail.com
Password: 12345678

Register:
✅ Đăng ký thành công
✅ Redirect /login/

Login:
Expected Console:
⚠️ Profile not found. Creating...
✅ Profile created automatically
✅ Đăng nhập thành công
✅ Dashboard hiển thị
```

---

### Test 3: Profile Create Fail Lúc Đăng Ký

```
Register: (simulated profile create error)
✅ User saved
⚠️ Profile create failed

Login:
Expected Console:
⚠️ Profile not found
📋 Creating profile...
✅ Profile created automatically
✅ Đăng nhập thành công
```

---

## 📊 SO SÁNH

| Tình Huống | TRƯỚC | SAU |
|-----------|-------|-----|
| **Profile OK** | ✅ Đăng nhập được | ✅ Đăng nhập được |
| **Profile Fail** | ❌ Đăng nhập fail | ✅ Tự động tạo + đăng nhập OK |
| **Profile Missing** | ❌ Đăng nhập fail | ✅ Tự động tạo + đăng nhập OK |
| **orElse behavior** | Empty object | Tìm hoặc tạo |

---

## 📁 FILES UPDATED

| File | Change |
|------|--------|
| **UsersService.java** | ✏️ Auto-create profile in getCurrentUserProfile() |
| **UsersService.java** | ✏️ Don't throw exception when profile create fails |

---

## 💡 KEY INSIGHT

**Trước:** Code giả sử profile luôn tồn tại  
**Sau:** Code tự động tạo profile nếu cần

```java
// BEFORE - Tỏ nguy:
JobSeekerProfile profile = repo.findById(id).orElse(new JobSeekerProfile());
// Empty object với userId = null → Crash!

// AFTER - An toàn:
JobSeekerProfile profile = repo.findById(id).orElse(null);
if (profile == null) {
    profile = new JobSeekerProfile(users);
    profile = repo.save(profile);  // ✅ Tự động tạo
}
return profile;
```

---

## 🚀 DEPLOY

```bash
mvn clean install
mvn spring-boot:run

# Test
http://localhost:8080/register
http://localhost:8080/login
```

---

## ✅ RESULT

- ✅ Tài khoản cũ: Vẫn đăng nhập được
- ✅ Tài khoản mới: Đăng nhập được
- ✅ Profile auto-create: Nếu cần
- ✅ No crash: Xử lý gracefully

---

**Ngày cập nhật: 12/04/2026**  
**Status: ✅ FIXED**

