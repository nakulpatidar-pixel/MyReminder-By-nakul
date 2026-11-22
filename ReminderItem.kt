package com.example.myreminderai

data class ReminderItem(
    val id: Long,
    val title: String,
    val whenMillis: Long,
    val done: Boolean = false,
    val whatsApp: Boolean = false
)
