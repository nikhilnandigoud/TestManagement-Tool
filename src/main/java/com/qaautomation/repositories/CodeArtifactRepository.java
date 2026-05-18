package com.qaautomation.repositories;

import com.qaautomation.models.CodeArtifact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CodeArtifactRepository extends JpaRepository<CodeArtifact, String> {
}
