package com.civiccms.repository;

import com.civiccms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByDepartmentId(Long departmentId);
    java.util.List<User> findByRole(User.Role role);
}
