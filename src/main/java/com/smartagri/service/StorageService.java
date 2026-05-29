package com.smartagri.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction over file storage backends.
 *
 * <p>Two implementations are provided:
 * <ul>
 *   <li>S3-backed (when {@code aws.enabled=true}) — uploads to an Amazon S3
 *       bucket and returns the public object URL.</li>
 *   <li>Local filesystem (when {@code aws.enabled=false}) — saves the file
 *       under {@code uploads/} and returns a servlet-relative path such as
 *       {@code /uploads/abc123.jpg}.</li>
 * </ul>
 *
 * <p>All implementations <strong>must</strong> validate:
 * <ul>
 *   <li>Maximum file size: 5 MB</li>
 *   <li>Allowed MIME types: {@code image/jpeg} and {@code image/png}</li>
 * </ul>
 */
public interface StorageService {

    /**
     * Validates, stores the file, and returns a resolvable URL.
     *
     * @param file   the multipart file received from the client
     * @param folder logical grouping subfolder (e.g. {@code "crop-photos"})
     * @return public URL or server-relative path of the stored file
     * @throws IllegalArgumentException if the file fails validation
     * @throws RuntimeException         if storage fails
     */
    String uploadFile(MultipartFile file, String folder);

    /**
     * Deletes the file identified by {@code fileUrl}.
     *
     * <p>For S3 the URL is parsed to extract the object key.
     * For local storage the path segment after {@code /uploads/} is used.
     * Implementations should log but not throw if the file no longer exists.
     *
     * @param fileUrl the URL returned by a previous {@link #uploadFile} call
     */
    void deleteFile(String fileUrl);
}
