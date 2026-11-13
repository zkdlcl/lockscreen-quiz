package com.example.lockscreenquiz

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.random.Random

class QuizRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("quiz_stats", Context.MODE_PRIVATE)
    private var questions: List<Question> = emptyList()
    private var yearFactsData: YearFactsData = YearFactsData()
    private var lastQuestionId: Int = -1

    init {
        loadQuestions()
        loadYearFacts()
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

    private fun loadYearFacts() {
        try {
            val jsonString = context.assets.open("ihl_years_facts.json")
                .bufferedReader()
                .use { it.readText() }
            yearFactsData = Gson().fromJson(jsonString, YearFactsData::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAllQuestions(): List<Question> = questions

    fun getYearFactsData(): YearFactsData = yearFactsData

    // 오답 가중치 + 핵심(core) 가중치 기반 랜덤 문제 추출
    fun getNextQuestion(): Question? {
        if (questions.isEmpty()) return null
        if (questions.size == 1) return questions.first()

        val weightedPool = mutableListOf<Question>()
        for (q in questions) {
            val wrongCount = getWrongCount(q.id)
            val baseWeight = if (q.core) 2 else 1
            val weight = baseWeight + (wrongCount * 3) // 오답 1회당 출제 확률 가중치 +3
            repeat(weight) {
                weightedPool.add(q)
            }
        }

        var chosen = weightedPool[Random.nextInt(weightedPool.size)]
        var attempts = 0
        while (chosen.id == lastQuestionId && attempts < 10) {
            chosen = weightedPool[Random.nextInt(weightedPool.size)]
            attempts++
        }
        lastQuestionId = chosen.id
        return chosen
    }

    fun recordResult(questionId: Int, isCorrect: Boolean) {
        val currentWrong = getWrongCount(questionId)
        val currentSolved = getSolvedCount(questionId)
        val editor = prefs.edit().putInt("solved_$questionId", currentSolved + 1)
        if (!isCorrect) {
            editor.putInt("wrong_$questionId", currentWrong + 1)
        } else if (currentWrong > 0) {
            // 맞히면 오답 카운트를 1씩 차감 (완전히 외우면 빈도 정상화)
            editor.putInt("wrong_$questionId", currentWrong - 1)
        }
        editor.apply()
    }

    fun getWrongCount(questionId: Int): Int {
        return prefs.getInt("wrong_$questionId", 0)
    }

    fun getSolvedCount(questionId: Int): Int {
        return prefs.getInt("solved_$questionId", 0)
    }

    fun getWrongQuestionsCount(): Int {
        return questions.count { getWrongCount(it.id) > 0 }
    }

    fun resetAllStats() {
        prefs.edit().clear().apply()
    }
}
