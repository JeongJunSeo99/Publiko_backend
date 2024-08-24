package com.kobot.backend.repository;

import com.kobot.backend.entity.HostUrl;
import com.kobot.backend.entity.SubUrls;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubUrlsRepository extends JpaRepository<SubUrls, Integer> {
    SubUrls findByHostUrlAndSubUrl(HostUrl hostUrl, String subUrl);

    SubUrls findBySubUrl(String subUrlKey);

    List<SubUrls> findByHostUrl(HostUrl hostUrl);

    boolean existsBySubUrlAndHostUrl(String subUrl, HostUrl hostUrl);
}
