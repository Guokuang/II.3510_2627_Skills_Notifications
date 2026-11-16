package com.isep.shared.domain.entities

data class AppNotification(
    val id: Int,
    val recipientId: Int,
    val listingId: Int,
    val message: String,
    val isRead: Boolean = false
)