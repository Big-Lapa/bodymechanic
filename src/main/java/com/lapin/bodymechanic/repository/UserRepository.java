package com.lapin.bodymechanic.repository;

import com.lapin.bodymechanic.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}
