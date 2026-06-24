package com.receiptvault.app.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.receiptvault.app.BuildConfig
import com.receiptvault.app.data.database.ReceiptVaultDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun export(passphrase: String): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val dbFile = context.getDatabasePath(ReceiptVaultDatabase.DATABASE_NAME)
            val stagingZip = File(context.cacheDir, "rv_backup_stage.zip")
            ZipOutputStream(FileOutputStream(stagingZip)).use { zip ->
                zip.putNextEntry(ZipEntry("receiptvault.db"))
                FileInputStream(dbFile).use { it.copyTo(zip) }
                zip.closeEntry()

                val manifest = JSONObject()
                    .put("version", 2)
                    .put("app", BuildConfig.VERSION_NAME)
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifest.toString().toByteArray())
                zip.closeEntry()

                val imagesDir = File(context.filesDir, "receipts")
                if (imagesDir.exists()) {
                    imagesDir.listFiles()?.forEach { file ->
                        zip.putNextEntry(ZipEntry("images/${file.name}"))
                        FileInputStream(file).use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }

            val encrypted = File(context.cacheDir, "receiptvault_backup.rvbak")
            encryptFile(stagingZip, encrypted, passphrase)
            stagingZip.delete()

            FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                encrypted
            )
        }
    }

    private fun encryptFile(input: File, output: File, passphrase: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(passphrase, salt)
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))

        FileOutputStream(output).use { fos ->
            fos.write(salt)
            fos.write(iv)
            CipherOutputStream(fos, cipher).use { cos ->
                FileInputStream(input).use { it.copyTo(cos) }
            }
        }
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = javax.crypto.spec.PBEKeySpec(passphrase.toCharArray(), salt, 100_000, 256)
        val key = factory.generateSecret(spec).encoded
        return SecretKeySpec(key, "AES")
    }
}

@Singleton
class BackupImporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun import(backupUri: Uri, passphrase: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val encrypted = File(context.cacheDir, "restore_in.rvbak")
                context.contentResolver.openInputStream(backupUri)?.use { input ->
                    FileOutputStream(encrypted).use { output -> input.copyTo(output) }
                } ?: error("Cannot read backup file")

                val decrypted = File(context.cacheDir, "restore_stage.zip")
                decryptFile(encrypted, decrypted, passphrase)
                encrypted.delete()

                val dbFile = context.getDatabasePath(ReceiptVaultDatabase.DATABASE_NAME)
                val imagesDir = File(context.filesDir, "receipts")
                if (!imagesDir.exists()) imagesDir.mkdirs()

                ZipInputStream(FileInputStream(decrypted)).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        when {
                            entry.name == "receiptvault.db" -> {
                                FileOutputStream(dbFile).use { zip.copyTo(it) }
                            }
                            entry.name.startsWith("images/") -> {
                                val name = entry.name.removePrefix("images/")
                                FileOutputStream(File(imagesDir, name)).use { zip.copyTo(it) }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
                decrypted.delete()
                Unit
            }
        }

    private fun decryptFile(input: File, output: File, passphrase: String) {
        FileInputStream(input).use { fis ->
            val salt = ByteArray(16)
            val iv = ByteArray(12)
            fis.read(salt)
            fis.read(iv)
            val key = deriveKey(passphrase, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            CipherOutputStream(FileOutputStream(output), cipher).use { cos ->
                fis.copyTo(cos)
            }
        }
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = javax.crypto.spec.PBEKeySpec(passphrase.toCharArray(), salt, 100_000, 256)
        val key = factory.generateSecret(spec).encoded
        return SecretKeySpec(key, "AES")
    }
}
