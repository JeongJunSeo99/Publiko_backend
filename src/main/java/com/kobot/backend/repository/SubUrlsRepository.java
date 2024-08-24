package com.kobot.backend.repository;

import com.kobot.backend.entity.HostUrl;
import com.kobot.backend.entity.SubUrls;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubUrlsRepository extends JpaRepository<SubUrls, Integer> {
    SubUrls findByHostUrlAndSubUrl(HostUrl hostUrl, String subUrl);
}
