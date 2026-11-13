package com.example.lockscreenquiz

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.random.Random

class QuizRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("quiz_stats", Context.MODE_PRIVATE)
    private var questions: List<Question> = emptyList()

    init {
        loadQuestions()
    }

    private fun loadQuestions() {
        try {
            val jsonString = context.assets.open("questions.json")
                .bufferedReader()
                .use { it.readText() }
            val listType = object : TypeToken<List<Question>>() {}.type
            questions = Gson().fromJson(jsonString, listType)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 오답 가중치 기반 랜덤 문제 추출
    fun getNextQuestion(): Question? {
        if (questions.isEmpty()) return null

        val weightedPool = mutableListOf<Question>()
        for (q in questions) {
            val wrongCount = getWrongCount(q.id)
            val weight = 1 + (wrongCount * 3) // 오답 1회당 출제 확률 가중치 +3
            repeat(weight) {
                weightedPool.add(q)
            }
        }
        return weightedPool[Random.nextInt(weightedPool.size)]
    }

    fun recordResult(questionId: Int, isCorrect: Boolean) {
        val currentWrong = getWrongCount(questionId)
        if (!isCorrect) {
            prefs.edit().putInt("wrong_$questionId", currentWrong + 1).apply()
        } else if (currentWrong > 0) {
            // 맞히면 오답 카운트를 1씩 차감 (완전히 외우면 빈도 정상화)
            prefs.edit().putInt("wrong_$questionId", currentWrong - 1).apply()
        }
    }

    fun getWrongCount(questionId: Int): Int {
        return prefs.getInt("wrong_$questionId", 0)
    }
}
