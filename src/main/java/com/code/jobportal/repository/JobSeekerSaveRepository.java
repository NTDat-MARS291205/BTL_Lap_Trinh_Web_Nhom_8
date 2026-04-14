package com.code.jobportal.repository;

import com.code.jobportal.entity.JobPostActivity;
import com.code.jobportal.entity.JobSeekerProfile;
import com.code.jobportal.entity.JobSeekerSave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobSeekerSaveRepository extends JpaRepository<JobSeekerSave, Integer> {

    List<JobSeekerSave> findByUserId(JobSeekerProfile userAccountId);

    List<JobSeekerSave> findByJob(JobPostActivity job);

    boolean existsByUserIdAndJob(JobSeekerProfile userId, JobPostActivity job);

    Optional<JobSeekerSave> findByUserIdAndJob(JobSeekerProfile userId, JobPostActivity job);

    long countByJob(JobPostActivity job);

    @Modifying
    @Transactional
    void deleteByJob_JobPostId(Integer jobPostId);

    @Modifying
    @Transactional
    void deleteByUserIdAndJob(JobSeekerProfile userId, JobPostActivity job);
}
