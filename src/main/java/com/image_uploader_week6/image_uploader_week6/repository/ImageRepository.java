package com.image_uploader_week6.image_uploader_week6.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.image_uploader_week6.image_uploader_week6.models.Image;

@Repository
public interface ImageRepository extends JpaRepository<Image, Integer> {
    void deleteByImageUrl(String url);
}
