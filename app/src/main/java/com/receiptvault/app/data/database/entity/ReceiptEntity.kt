package com.receiptvault.app.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room table for receipts. The foreign key to [FolderEntity] uses SET_NULL so deleting a
 * folder un-files its receipts rather than deleting them. Indices back the foreign key and
 * the most common sort/filter columns.
 */
@Entity(
    tableName = "receipts",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("folderId"),
        Index("date"),
        Index("merchantName")
    ]
)
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val merchantName: String?,
    val amount: Double?,
    val taxAmount: Double?,
    val date: Long,
    val category: String?,
    val notes: String?,
    val folderId: Long?,
    val imagePath: String?,
    val pdfPath: String?,
    val createdAt: Long,
    val updatedAt: Long
)
