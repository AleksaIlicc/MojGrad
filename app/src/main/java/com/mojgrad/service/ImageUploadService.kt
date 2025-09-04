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

    suspend fun uploadImage(
        imageUri: Uri,
        folder: String = "uploads",
        fileName: String? = null
    ): Result<FileUploadResponse> = withContext(Dispatchers.IO) {
        try {
            val file = convertUriToFile(imageUri)

            try {
                val extension = getFileExtension(file.name)
                val uniqueId = UUID.randomUUID().toString()
                val finalFileName = fileName ?: "${uniqueId}${extension}"
                val objectKey = if (folder.isNotEmpty()) "$folder/$finalFileName" else finalFileName

                Log.d(TAG, "Uploading file with key: $objectKey")

                val putRequest = PutObjectRequest {
                    bucket = bucketName
                    key = objectKey
                    body = ByteStream.fromBytes(file.readBytes())
                    contentType = "image/jpeg"
                    metadata = mapOf(
                        "uploaded-by" to "mojgrad-android"
                    )
                }

                Log.d(TAG, "Starting S3 putObject call...")
                val putResult = s3Client.putObject(putRequest)
                Log.d(TAG, "S3 putObject completed successfully")

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
                file.delete()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image", e)
            Result.failure(e)
        }
    }


    private fun getPublicUrl(key: String): String {
        return if (publicUrl.isNotEmpty()) {
            if (publicUrl.endsWith("/")) {
                "$publicUrl$key"
            } else {
                "$publicUrl/$key"
            }
        } else {
            key
        }
    }

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

    private fun convertUriToFile(uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open input stream for URI: $uri")

        val fileName = "upload_${UUID.randomUUID()}.jpg"
        val tempFile = File(context.cacheDir, fileName)

        FileOutputStream(tempFile).use { outputStream ->
            inputStream.use { input ->
                input.copyTo(outputStream)
            }
        }

        return tempFile
    }

    private fun getFileExtension(fileName: String): String {
        val lastDotIndex = fileName.lastIndexOf('.')
        return if (lastDotIndex > 0) {
            fileName.substring(lastDotIndex)
        } else {
            ".jpg"
        }
    }

    fun close() {
        s3Client.close()
    }
}
