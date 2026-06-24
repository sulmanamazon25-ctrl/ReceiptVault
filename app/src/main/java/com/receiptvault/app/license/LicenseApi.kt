package com.receiptvault.app.license

import com.receiptvault.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class ActivateLicenseResponse(
    val tier: String,
    val licenseKeyId: String,
    val token: String,
    val tokenExpiresAt: Long
)

@Singleton
class LicenseApi @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun activate(
        licenseKey: String,
        deviceHash: String,
        deviceLabel: String?,
        appVersion: String
    ): Result<ActivateLicenseResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("license_key", licenseKey)
                .put("device_hash", deviceHash)
                .put("device_label", deviceLabel)
                .put("app_version", appVersion)
                .toString()

            val request = Request.Builder()
                .url(BuildConfig.ACTIVATE_LICENSE_URL)
                .post(body.toRequestBody(jsonType))
                .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string() ?: ""
                val json = JSONObject(text)
                if (!response.isSuccessful) {
                    error(json.optString("error", "Activation failed (${response.code})"))
                }
                ActivateLicenseResponse(
                    tier = json.getString("tier"),
                    licenseKeyId = json.getString("license_key_id"),
                    token = json.getString("token"),
                    tokenExpiresAt = json.getLong("token_expires_at")
                )
            }
        }
    }

    suspend fun check(licenseKeyId: String, deviceHash: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = JSONObject()
                    .put("license_key_id", licenseKeyId)
                    .put("device_hash", deviceHash)
                    .toString()

                val request = Request.Builder()
                    .url(BuildConfig.CHECK_LICENSE_URL)
                    .post(body.toRequestBody(jsonType))
                    .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .build()

                client.newCall(request).execute().use { response ->
                    val text = response.body?.string() ?: ""
                    val json = JSONObject(text)
                    if (!response.isSuccessful) {
                        error(json.optString("error", "Check failed"))
                    }
                    json.optBoolean("valid", false)
                }
            }
        }
}
