package com.image_uploader_week6.image_uploader_week6.services;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.image_uploader_week6.image_uploader_week6.models.Image;
import com.image_uploader_week6.image_uploader_week6.repository.ImageRepository;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
public class ImageService {
    
    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Presigner s3Presigner;

    @Autowired
    private ImageRepository imageRepository;

    private String BUCKET_NAME = "week6imageuploaderbucket";
    // private String BUCKET_NAME = "week5labbucketimageuploader";

    
    // Map to store pagination state
    // private Map<Integer, String> pageTokenMap = new HashMap<>();

    // public Map<String, Object> getImages(int page, int size) {
    //     Map<String, Object> result = new HashMap<>();
    //     List<String> imageUrls = new ArrayList<>();
        
    //     // Get continuation token for the requested page
    //     String continuationToken = page > 0 ? pageTokenMap.get(page - 1) : null;
        
    //     ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
    //             .bucket(BUCKET_NAME)
    //             .maxKeys(size);
                
    //     if (continuationToken != null && !continuationToken.isEmpty()) {
    //         requestBuilder.continuationToken(continuationToken);
    //     }
        
    //     ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());
        
    //     // Process images and generate presigned URLs
    //     List<S3Object> objects = response.contents();
    //     imageUrls = objects.stream()
    //             .filter(s3Object -> isImage(s3Object.key()))
    //             .map(obj -> generatePresignedUrl(obj.key()))
    //             .collect(Collectors.toList());
        
    //     // Store the next continuation token for subsequent pages
    //     if (response.isTruncated()) {
    //         pageTokenMap.put(page, response.nextContinuationToken());
    //         result.put("hasNextPage", true);
    //     } else {
    //         result.put("hasNextPage", false);
    //     }
        
    //     // Calculate total count for proper pagination display
    //     long totalCount = countTotalImages();
    //     int totalPages = (int) Math.ceil((double) totalCount / size);
        
    //     result.put("images", imageUrls);
    //     result.put("totalPages", Math.max(1, totalPages));
    //     result.put("currentPage", page);
        
    //     return result;
    // }


    public Map<String, Object> getImages(int page, int size) {
        Map<String, Object> result = new HashMap<>();
        
        // Handle pagination
        Pageable pageable = PageRequest.of(page, size);
        
        // Get images from the database using the repository
        Page<Image> imagePage = imageRepository.findAll(pageable);
        
        // Extract the image URLs from the database
        // List<String> imageUrls = imagePage.getContent().stream()
        //         .map(Image::getImageUrl)  // Assuming the method getImageUrl() fetches the S3 URL from the database
        //         .collect(Collectors.toList());
        
        // // Generate presigned URLs if necessary (for private images)
        // List<String> presignedUrls = imageUrls.stream()
        //         .map(this::generatePresignedUrl)
        //         .collect(Collectors.toList());

        List<Map<String, String>> imageDetails = imagePage.getContent().stream()
            .map(image -> {
                Map<String, String> imageDetail = new HashMap<>();
                String presignedUrl = generatePresignedUrl(image.getImageUrl());  // Generate presigned URL for private images
                imageDetail.put("url", presignedUrl);
                imageDetail.put("description", image.getImageDescription());  // Assuming the method getImageDescription() fetches the description from the database
                return imageDetail;
            })
            .collect(Collectors.toList());

        
        // Store pagination details
        result.put("images", imageDetails);  // Return the presigned URLs instead of raw S3 URLs
        result.put("currentPage", page);
        result.put("totalPages", imagePage.getTotalPages());
        result.put("hasNextPage", imagePage.hasNext());
        
        return result;
    }


    // private long countTotalImages() {
    //     long count = 0;
    //     String continuationToken = null;
        
    //     do {
    //         ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
    //                 .bucket(BUCKET_NAME);
                    
    //         if (continuationToken != null) {
    //             requestBuilder.continuationToken(continuationToken);
    //         }
            
    //         ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());
            
    //         count += response.contents().stream()
    //                 .filter(s3Object -> isImage(s3Object.key()))
    //                 .count();
                    
    //         continuationToken = response.isTruncated() ? response.nextContinuationToken() : null;
    //     } while (continuationToken != null);
        
    //     return count;
    // }

    public String generatePresignedUrl(String objectKey) {
        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key(objectKey)
                        .build())
                .build();
        
        PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(getObjectPresignRequest);
        return presignedGetObjectRequest.url().toString();
    }

    // private boolean isImage(String key) {
    //     return key.toLowerCase().endsWith(".jpg") || 
    //            key.toLowerCase().endsWith(".jpeg") || 
    //            key.toLowerCase().endsWith(".png");
    // }


    public String uploadMultipleFiles(MultipartFile[] files, String description) throws IOException {
        // Filter out empty files
        List<MultipartFile> nonEmptyFiles = Arrays.stream(files)
                .filter(file -> !file.isEmpty())
                .collect(Collectors.toList());
        
        // If all files were empty, return an appropriate response
        if (nonEmptyFiles.isEmpty()) {
            return "empty";
        }
        
        if (nonEmptyFiles.size() > 5) {
            return "max";
        }
        
        for (MultipartFile file : nonEmptyFiles) {
            // check file size
            if (file.getSize() > 1000000) {
                return "size";
            }
            String key = "image_" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
            String imageUrl = key;
            Image image = new Image();
            image.setImageDescription(description);
            image.setImageUrl(imageUrl);
            imageRepository.save(image);
        }
    
        return "success";
    }


    @Transactional
    public String deleteImage(String id) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                                                                            .bucket(BUCKET_NAME)
                                                                            .key(id)
                                                                            .build();
                                            
            s3Client.deleteObject(deleteObjectRequest);
            imageRepository.deleteByImageUrl(id);

            return "success";
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return "error";
        }
    }
}
