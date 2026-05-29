package com.smartagri.service.impl;

import com.smartagri.service.StorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * {@link StorageService} implementation that supports two storage backends,
 * selected at startup by the {@code aws.enabled} property.
 *
 * <h3>S3 mode ({@code aws.enabled=true})</h3>
 * Uses the AWS SDK v2 {@code S3Client} to upload objects with public-read ACL
 * and returns the canonical public URL.
 *
 * <h3>Local mode ({@code aws.enabled=false})</h3>
 * Saves files under the {@code uploads/} directory relative to the working
 * directory and returns a servlet-relative path (e.g. {@code /uploads/abc.jpg}).
 * Useful for local development without AWS credentials.
 *
 * <h3>Validation (both modes)</h3>
 * <ul>
 *   <li>Maximum file size: 5 MB</li>
 *   <li>Allowed content types: {@code image/jpeg}, {@code image/png}</li>
 * </ul>
 */
@Slf4j
@Service
public class StorageServiceImpl implements StorageService {

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final long  MAX_BYTES          = 5L * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png");
    private static final String LOCAL_UPLOAD_DIR  = "uploads";

    // ── Configuration ─────────────────────────────────────────────────────────

    @Value("${aws.enabled:false}")
    private boolean awsEnabled;

    @Value("${aws.bucket:smart-agri-photos}")
    private String bucket;

    @Value("${aws.region:ap-south-1}")
    private String region;

    @Value("${aws.access-key:}")
    private String accessKey;

    @Value("${aws.secret-key:}")
    private String secretKey;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @PostConstruct
    void init() {
        if (!awsEnabled) {
            Path uploadPath = Paths.get(LOCAL_UPLOAD_DIR);
            try {
                Files.createDirectories(uploadPath);
                log.info("Local storage mode — upload directory: {}",
                        uploadPath.toAbsolutePath());
            } catch (IOException e) {
                log.warn("Could not create local upload directory {}: {}",
                        uploadPath.toAbsolutePath(), e.getMessage());
            }
        } else {
            log.info("AWS S3 storage mode — bucket={}, region={}", bucket, region);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // StorageService implementation
    // ═════════════════════════════════════════════════════════════════════════

    /** {@inheritDoc} */
    @Override
    public String uploadFile(MultipartFile file, String folder) {
        validateFile(file);

        String uniqueFileName = buildUniqueFileName(file.getOriginalFilename());
        String objectKey = folder + "/" + uniqueFileName;

        if (awsEnabled) {
            return uploadToS3(file, objectKey);
        } else {
            return saveLocally(file, uniqueFileName);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;

        if (awsEnabled) {
            deleteFromS3(fileUrl);
        } else {
            deleteLocally(fileUrl);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Private helpers – validation
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Ensures the file is non-empty, within size limits, and an allowed type.
     *
     * @throws IllegalArgumentException on any violation
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException(
                    "File size exceeds the 5 MB limit (received "
                    + file.getSize() / 1024 + " KB)");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Invalid file type '" + contentType
                    + "'. Only JPG and PNG images are allowed.");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Private helpers – filename
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Generates a UUID-prefixed filename preserving the original extension.
     * Falls back to {@code .jpg} if the original filename has no extension.
     */
    private String buildUniqueFileName(String originalFilename) {
        String extension = ".jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        return UUID.randomUUID().toString().replace("-", "") + extension;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Private helpers – S3 storage
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Uploads the file to S3 and returns its public URL.
     *
     * <p>The AWS SDK v2 classes are referenced by their fully-qualified names
     * to avoid compile-time failures when the SDK JAR is absent (e.g. during
     * unit tests with {@code aws.enabled=false}).
     */
    private String uploadToS3(MultipartFile file, String objectKey) {
        try {
            software.amazon.awssdk.regions.Region awsRegion =
                    software.amazon.awssdk.regions.Region.of(region);

            software.amazon.awssdk.auth.credentials.AwsBasicCredentials credentials =
                    software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(
                            accessKey, secretKey);

            software.amazon.awssdk.services.s3.S3Client s3 =
                    software.amazon.awssdk.services.s3.S3Client.builder()
                            .region(awsRegion)
                            .credentialsProvider(
                                    software.amazon.awssdk.auth.credentials
                                            .StaticCredentialsProvider.create(credentials))
                            .build();

            software.amazon.awssdk.services.s3.model.PutObjectRequest putRequest =
                    software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectKey)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build();

            s3.putObject(putRequest,
                    software.amazon.awssdk.core.sync.RequestBody.fromBytes(file.getBytes()));
            s3.close();

            String url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + objectKey;
            log.info("Uploaded file to S3: {}", url);
            return url;

        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file bytes: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("S3 upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts the S3 object key from the public URL and deletes the object.
     * Logs a warning instead of throwing if the object is already gone.
     */
    private void deleteFromS3(String fileUrl) {
        try {
            // URL format: https://<bucket>.s3.<region>.amazonaws.com/<key>
            URI uri = URI.create(fileUrl);
            String objectKey = uri.getPath().replaceFirst("^/", "");

            software.amazon.awssdk.regions.Region awsRegion =
                    software.amazon.awssdk.regions.Region.of(region);

            software.amazon.awssdk.auth.credentials.AwsBasicCredentials credentials =
                    software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(
                            accessKey, secretKey);

            software.amazon.awssdk.services.s3.S3Client s3 =
                    software.amazon.awssdk.services.s3.S3Client.builder()
                            .region(awsRegion)
                            .credentialsProvider(
                                    software.amazon.awssdk.auth.credentials
                                            .StaticCredentialsProvider.create(credentials))
                            .build();

            software.amazon.awssdk.services.s3.model.DeleteObjectRequest deleteRequest =
                    software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectKey)
                            .build();

            s3.deleteObject(deleteRequest);
            s3.close();
            log.info("Deleted S3 object: key={}", objectKey);

        } catch (Exception e) {
            log.warn("Failed to delete S3 object for URL {}: {}", fileUrl, e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Private helpers – local filesystem storage
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Saves the file to the local {@code uploads/} directory.
     *
     * @return servlet-relative URL: {@code /uploads/<uniqueFileName>}
     */
    private String saveLocally(MultipartFile file, String uniqueFileName) {
        try {
            Path uploadPath = Paths.get(LOCAL_UPLOAD_DIR);
            Files.createDirectories(uploadPath);
            Path destination = uploadPath.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            String url = "/uploads/" + uniqueFileName;
            log.info("Saved file locally: {}", destination.toAbsolutePath());
            return url;
        } catch (IOException e) {
            throw new RuntimeException("Local file save failed: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a locally-stored file identified by its servlet path.
     * Logs a warning if the file does not exist; does not throw.
     */
    private void deleteLocally(String fileUrl) {
        try {
            // fileUrl is like /uploads/abc123.jpg
            String filename = fileUrl.replaceFirst("^/uploads/", "");
            Path filePath = Paths.get(LOCAL_UPLOAD_DIR, filename);
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("Deleted local file: {}", filePath.toAbsolutePath());
            } else {
                log.warn("Local file not found for deletion: {}", filePath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.warn("Failed to delete local file {}: {}", fileUrl, e.getMessage());
        }
    }
}
