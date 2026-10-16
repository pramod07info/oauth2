package com.pramod.oauth2.repository;

import com.pramod.oauth2.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAuditJpaRepository extends JpaRepository<UserAudit, java.util.UUID> {}
