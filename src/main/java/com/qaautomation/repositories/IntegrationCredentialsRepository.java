package com.qaautomation.repositories;

import com.qaautomation.models.IntegrationCredentials;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationCredentialsRepository extends JpaRepository<IntegrationCredentials, String> {
}
