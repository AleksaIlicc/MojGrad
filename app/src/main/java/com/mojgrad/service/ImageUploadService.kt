package com.mojgrad.service

import android.content.Context
import android.net.Uri
import android.util.Log
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.net.url.Url
import com.mojgrad.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class FileUploadResponse(
    val url: String,
    val key: String,
    val size: Long,
    val contentType: String
)

class ImageUploadService(private val context: Context) {
    
    private val TAG = "ImageUploadService"
    
    init {
        // Set AWS credentials as system properties for the Kotlin SDK
        System.setProperty("aws.accessKeyId", BuildConfig.R2_ACCESS_KEY)
        System.setProperty("aws.secretAccessKey", BuildConfig.R2_SECRET_KEY)
        
        Log.d(TAG, "AWS Credentials set:")
        Log.d(TAG, "Access Key: ${BuildConfig.R2_ACCESS_KEY.take(5)}...")
        Log.d(TAG, "Secret Key: ${BuildConfig.R2_SECRET_KEY.take(5)}...")
        Log.d(TAG, "Bucket: ${BuildConfig.R2_BUCKET_NAME}")
        Log.d(TAG, "Endpoint: ${BuildConfig.R2_ENDPOINT}")
        Log.d(TAG, "Public URL: ${BuildConfig.R2_PUBLIC_URL}")
    }
    
    private val s3Client = S3Client {
        region = "us-east-1"
        endpointUrl = Url.parse(BuildConfig.R2_ENDPOINT)
    }
    
    private val bucketName = BuildConfig.R2_BUCKET_NAME
    private val publicUrl = BuildConfig.R2_PUBLIC_URL
    
    /**
     * Uploads an image to Cloudflare R2 storage
     * @param imageUri URI of the image to upload
     * @param folder Optional folder prefix (default: "uploads")
     * @param fileName Optional custom filename (default: auto-generated)
     * @return Result containing the upload response or an error
     */
    suspend fun uploadImage(
        imageUri: Uri, 
        folder: String = "uploads",
        fileName: String? = null
    ): Result<FileUploadResponse> = withContext(Dispatchers.IO) {
        try {
            // Convert URI to File
            val file = convertUriToFile(imageUri)
            
            try {
                // Generate unique key for the object
                val extension = getFileExtension(file.name)
                val uniqueId = UUID.randomUUID().toString()
                val finalFileName = fileName ?: "${uniqueId}${extension}"
                val objectKey = if (folder.isNotEmpty()) "$folder/$finalFileName" else finalFileName
                
                Log.d(TAG, "Uploading file with key: $objectKey")
                
                // Create upload request using AWS SDK for Kotlin
                val putRequest = PutObjectRequest {
                    bucket = bucketName
                    key = objectKey
                    body = ByteStream.fromBytes(file.readBytes())
                    contentType = "image/jpeg"
                    // ACL is set to public-read by default in R2
                    metadata = mapOf(
                        "uploaded-by" to "mojgrad-android"
                    )
                }
                
                Log.d(TAG, "Starting S3 putObject call...")
                // Upload file (enableAwsChunked=false prevents the STREAMING error)
                val putResult = s3Client.putObject(putRequest)
                Log.d(TAG, "S3 putObject completed successfully")
                
                // Get public URL
                val url = getPublicUrl(objectKey)
                Log.d(TAG, "Generated public URL: $url")
                
                val response = FileUploadResponse(
                    url = url,
                    key = objectKey,
                    size = file.length(),
                    contentType = "image/jpeg"
                )
                
                Log.d(TAG, "Upload successful: $url")
                Result.success(response)
                
            } finally {
                // Clean up temporary file
                file.delete()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get public URL for a file
     */
    private fun getPublicUrl(key: String): String {
        return if (publicUrl.isNotEmpty()) {
            if (publicUrl.endsWith("/")) {
                "$publicUrl$key"
            } else {
                "$publicUrl/$key"
            }
        } else {
            // Fallback URL format for R2
            "https://$bucketName.r2.cloudflarestorage.com/$key"
        }
    }
    
    /**
     * Delete a file from R2
     */
    suspend fun deleteFile(key: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val deleteRequest = DeleteObjectRequest {
                bucket = bucketName
                this.key = key
            }
            s3Client.deleteObject(deleteRequest)
            Log.d(TAG, "File deleted successfully: $key")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file: $key", e)
            Result.failure(e)
        }
    }
    
    /**
     * Converts a content URI to a temporary File
     */
    private fun convertUriToFile(uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open input stream for URI: $uri")
        
        // Create temp file
        val fileName = "upload_${UUID.randomUUID()}.jpg"
        val tempFile = File(context.cacheDir, fileName)
        
        FileOutputStream(tempFile).use { outputStream ->
            inputStream.use { input ->
                input.copyTo(outputStream)
            }
        }
        
        return tempFile
    }
    
    /**
     * Get file extension from filename
     */
    private fun getFileExtension(fileName: String): String {
        val lastDotIndex = fileName.lastIndexOf('.')
        return if (lastDotIndex > 0) {
            fileName.substring(lastDotIndex)
        } else {
            ".jpg"
        }
    }
    
    /**
     * Close the S3 client to free resources
     */
    fun close() {
        s3Client.close()
    }
}
