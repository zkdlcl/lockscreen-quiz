package com.example.lockscreenquiz

data class Question(
    val id: Int,
    val question: String,
    val options: List<String>,
    val answer: Int, // 0, 1, 2, 3
    val explanation: String
)
