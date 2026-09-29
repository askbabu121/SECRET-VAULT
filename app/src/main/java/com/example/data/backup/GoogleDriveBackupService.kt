package com.example.data.backup

import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class GoogleDriveBackupFile(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val modifiedTime: String,
    val description: String?
) {
    val formattedDate: String
        get() {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                val date = inputFormat.parse(modifiedTime)
                if (date != null) {
                    SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault()).format(date)
                } else modifiedTime
            } catch (e: Exception) {
                modifiedTime
            }
        }

    val formattedSize: String
        get() {
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) {
                String.format(Locale.US, "%.1f MB", mb)
            } else {
                String.format(Locale.US, "%.1f KB", kb)
            }
        }
}

class GoogleDriveBackupService(
    private val context: Context,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val OAUTH_SCOPE_STRING = "oauth2:$DRIVE_FILE_SCOPE $DRIVE_APPDATA_SCOPE"
    }

    fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DRIVE_FILE_SCOPE), Scope(DRIVE_APPDATA_SCOPE))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getSignedInAccount(): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return if (account != null && GoogleSignIn.hasPermissions(account, Scope(DRIVE_FILE_SCOPE), Scope(DRIVE_APPDATA_SCOPE))) {
            account
        } else {
            null
        }
    }

    suspend fun getAccessToken(account: GoogleSignInAccount): String = withContext(Dispatchers.IO) {
        val androidAccount = account.account ?: throw IllegalStateException("Google account not found")
        GoogleAuthUtil.getToken(context, androidAccount, OAUTH_SCOPE_STRING)
    }

    suspend fun uploadEncryptedBackup(
        accessToken: String,
        backupFile: File,
        fileName: String,
        description: String
    ): GoogleDriveBackupFile = withContext(Dispatchers.IO) {
        val metadataJson = JSONObject().apply {
            put("name", fileName)
            put("description", description)
            put("mimeType", "application/octet-stream")
            put("parents", JSONArray().put("appDataFolder"))
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addPart(
                metadataJson.toString().toRequestBody("application/json; charset=UTF-8".toMediaType())
            )
            .addPart(
                backupFile.asRequestBody("application/octet-stream".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
            .header("Authorization", "Bearer $accessToken")
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw IllegalStateException("Drive upload failed (${response.code}): $responseBody")
        }

        val resJson = JSONObject(responseBody)
        GoogleDriveBackupFile(
            id = resJson.getString("id"),
            name = resJson.optString("name", fileName),
            sizeBytes = backupFile.length(),
            modifiedTime = resJson.optString("modifiedTime", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())),
            description = description
        )
    }

    suspend fun listBackups(accessToken: String): List<GoogleDriveBackupFile> = withContext(Dispatchers.IO) {
        val url = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&fields=files(id,name,size,createdTime,modifiedTime,description)&orderBy=modifiedTime%20desc"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw IllegalStateException("Failed to query Drive backups (${response.code}): $responseBody")
        }

        val resJson = JSONObject(responseBody)
        val filesArray = resJson.optJSONArray("files") ?: JSONArray()
        val list = mutableListOf<GoogleDriveBackupFile>()

        for (i in 0 until filesArray.length()) {
            val item = filesArray.getJSONObject(i)
            val name = item.optString("name", "")
            if (name.endsWith(".vaultenc") || name.contains("vault_backup")) {
                list.add(
                    GoogleDriveBackupFile(
                        id = item.getString("id"),
                        name = name,
                        sizeBytes = item.optLong("size", 0L),
                        modifiedTime = item.optString("modifiedTime", item.optString("createdTime", "")),
                        description = item.optString("description", null)
                    )
                )
            }
        }
        list
    }

    suspend fun downloadBackupFile(
        accessToken: String,
        fileId: String,
        destinationFile: File
    ): File = withContext(Dispatchers.IO) {
        val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val err = response.body?.string() ?: ""
            throw IllegalStateException("Failed to download Drive backup (${response.code}): $err")
        }

        val body = response.body ?: throw IllegalStateException("Empty response body from Drive")
        destinationFile.parentFile?.mkdirs()
        FileOutputStream(destinationFile).use { out ->
            body.byteStream().use { input ->
                input.copyTo(out)
            }
        }
        destinationFile
    }

    suspend fun deleteBackupFile(accessToken: String, fileId: String): Boolean = withContext(Dispatchers.IO) {
        val url = "https://www.googleapis.com/drive/v3/files/$fileId"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .delete()
            .build()

        val response = okHttpClient.newCall(request).execute()
        response.isSuccessful
    }
}
