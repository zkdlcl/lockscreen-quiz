package com.example.lockscreenquiz

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class QuizActivity : ComponentActivity() {
    private lateinit var repository: QuizRepository
    private var newQuestionTrigger by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupLockScreenFlags()
        repository = QuizRepository(this)

        setContent {
            MaterialTheme {
                QuizScreen(
                    repository = repository,
                    triggerKey = newQuestionTrigger,
                    onUnlock = { unlockAndClose() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        newQuestionTrigger++
    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun unlockAndClose() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val km = getSystemService(KeyguardManager::class.java)
                if (km != null && km.isKeyguardLocked) {
                    km.requestDismissKeyguard(this, null)
                }
            }
        } catch (_: Exception) {
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        } else {
            finish()
        }
    }
}

@Composable
fun QuizScreen(
    repository: QuizRepository,
    triggerKey: Int = 0,
    onUnlock: () -> Unit
) {
    var currentQuestion by remember(triggerKey) { mutableStateOf(repository.getNextQuestion()) }
    var selectedOption by remember(triggerKey) { mutableStateOf<Int?>(null) }
    var isSubmitted by remember(triggerKey) { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val question = currentQuestion
    if (question == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = onUnlock) { Text("문제 데이터가 없습니다. 닫기") }
        }
        return
    }

    val wrongCount = repository.getWrongCount(question.id)
    val totalCount = repository.getAllQuestions().size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // 상단 고정 헤더 영역 (문제 번호, 배지, 우측 상단 즉시 닫기 버튼)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "문제 #${question.id} / $totalCount",
                    color = Color(0xFF90CAF9),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                if (question.core) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFB300).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "★ 핵심빈출",
                            color = Color(0xFFFFD54F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (!question.subject.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF263238), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = question.subject,
                            color = Color(0xFFB0BEC5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (wrongCount > 0) {
                    Text(
                        text = "오답 ${wrongCount}회",
                        color = Color(0xFFEF9A9A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                TextButton(
                    onClick = onUnlock,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "✕ 닫기",
                        color = Color(0xFFB0BEC5),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 중앙 스크롤 영역 (문제 지문 + 보기 + 해설)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            // 문제 지문
            Text(
                text = question.question,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 객관식 보기 목록
            question.options.forEachIndexed { index, optionText ->
                val isCorrect = index == question.answer
                val isSelected = selectedOption == index

                val borderColor = when {
                    !isSubmitted -> if (isSelected) Color(0xFF64B5F6) else Color(0xFF333333)
                    isCorrect -> Color(0xFF4CAF50) // 정답: 초록색
                    isSelected && !isCorrect -> Color(0xFFE53935) // 내가 고른 오답: 빨간색
                    else -> Color(0xFF2C2C2C)
                }

                val bgColor = when {
                    !isSubmitted -> Color(0xFF1E1E1E)
                    isCorrect -> Color(0xFF1B3320)
                    isSelected && !isCorrect -> Color(0xFF3B1A1A)
                    else -> Color(0xFF1E1E1E)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .background(bgColor, RoundedCornerShape(10.dp))
                        .border(2.dp, borderColor, RoundedCornerShape(10.dp))
                        .clickable(enabled = !isSubmitted) {
                            selectedOption = index
                            isSubmitted = true
                            repository.recordResult(question.id, isCorrect)
                        }
                        .padding(16.dp)
                ) {
                    Text(
                        text = "${index + 1}. $optionText",
                        color = Color.White,
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    )
                }
            }

            // 해설 영역 (보기 선택 즉시 노출)
            if (isSubmitted) {
                Spacer(modifier = Modifier.height(16.dp))
                val isUserCorrect = selectedOption == question.answer
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF263238)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isUserCorrect) "정답입니다!" else "오답입니다 (정답: ${question.answer + 1}번 · ${question.options.getOrNull(question.answer) ?: ""})",
                            color = if (isUserCorrect) Color(0xFF81C784) else Color(0xFFE57373),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = question.explanation,
                            color = Color(0xFFECEFF1),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        if (!question.source.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "출처: ${question.source}",
                                color = Color(0xFF90A4AE),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 하단 고정 액션 버튼 영역 (스크롤 영역 바깥에 고정 배치 & 언제든 닫기 가능)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    currentQuestion = repository.getNextQuestion()
                    selectedOption = null
                    isSubmitted = false
                },
                modifier = Modifier.weight(1f).height(52.dp)
            ) {
                Text("다음 문제", color = Color.White)
            }

            Button(
                onClick = onUnlock,
                enabled = true, // 문제를 풀지 않아도 급할 때 언제든 즉시 닫을 수 있도록 상시 활성화
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSubmitted) Color(0xFF1976D2) else Color(0xFF455A64)
                ),
                modifier = Modifier.weight(1f).height(52.dp)
            ) {
                Text("잠금해제 (닫기)", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
