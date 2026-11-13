package com.example.lockscreenquiz

data class Question(
    val id: Int,
    val question: String,
    val options: List<String>,
    val answer: Int, // 0, 1, 2, 3
    val explanation: String,
    val subject: String? = null,
    val source: String? = null,
    val core: Boolean = false
)

data class YearFact(
    val id: Int,
    val yearText: String,
    val subject: String,
    val event: String,
    val explanation: String,
    val source: String,
    val core: Boolean = false,
    val inRepo: Boolean = true
)

data class NotInQuizFact(
    val yearText: String,
    val event: String,
    val source: String,
    val note: String
)

data class YearFactsData(
    val facts: List<YearFact> = emptyList(),
    val notInQuiz: List<NotInQuizFact> = emptyList()
)
