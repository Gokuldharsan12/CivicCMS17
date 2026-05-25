package com.civiccms.repository;

import com.civiccms.entity.SiteContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteContentRepository extends JpaRepository<SiteContent, Long> {

    List<SiteContent> findByPageOrderByContentKey(String page);

    Optional<SiteContent> findByPageAndContentKey(String page, String contentKey);

    List<SiteContent> findAllByOrderByPageAscContentKeyAsc();
}
