package com.kobot.backend.repository;

import com.kobot.backend.entity.HostUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HostUrlRepository extends JpaRepository<HostUrl, Integer> {
    HostUrl findByHostUrl(String hostUrl);
}
