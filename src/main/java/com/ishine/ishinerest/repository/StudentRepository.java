package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.Student;
import com.ishine.ishinerest.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    
    // Query via user relationship since email is in users table, not students table
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Student s WHERE s.user.email = :email")
    boolean existsByEmail(@Param("email") String email);
    
    @Query("SELECT s FROM Student s WHERE LOWER(s.user.email) = LOWER(:email)")
    Optional<Student> findByEmailIgnoreCase(@Param("email") String email);
    
    Optional<Student> findByUser(User user);
}