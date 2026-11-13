package com.example.lockscreenquiz

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val repository = QuizRepository(this)

        setContent {
            MaterialTheme {
                var currentTab by remember { mutableStateOf("home") }
                var statsTick by remember { mutableStateOf(0) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212)
                ) {
                    if (currentTab == "home") {
                        val allQuestions = remember(statsTick) { repository.getAllQuestions() }
                        val coreCount = remember(statsTick) { allQuestions.count { it.core } }
                        val wrongCount = remember(statsTick) { repository.getWrongQuestionsCount() }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "승진고시 잠금화면 퀴즈",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "국제인도법 · 적십자 규약 · 조직법 · 정관 연도 완벽 대비",
                                fontSize = 13.sp,
                                color = Color(0xFF90CAF9)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // 문제은행 통계 요약 카드
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "수록 문제 은행 현황",
                                        color = Color(0xFF90CAF9),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "• 전체 퀴즈: ${allQuestions.size}문항 (핵심 빈출 ${coreCount}문항)",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "• 연도·법령 팩트 카드: ${repository.getYearFactsData().facts.size}개 + 보충 ${repository.getYearFactsData().notInQuiz.size}개",
                                        color = Color(0xFFB0BEC5),
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "• 현재 집중 출제 대상(오답 누적): ${wrongCount}문항",
                                        color = if (wrongCount > 0) Color(0xFFEF9A9A) else Color(0xFF81C784),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { checkOverlayPermission() },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3949AB))
                            ) {
                                Text("1단계: '다른 앱 위에 표시' 권한 허용", color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { startQuizService() },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                            ) {
                                Text("2단계: 잠금화면 퀴즈 서비스 시작 (ON)", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = { stopQuizService() },
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("잠금화면 퀴즈 서비스 중지 (OFF)", color = Color(0xFFB0BEC5))
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    startActivity(Intent(this@MainActivity, QuizActivity::class.java))
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                            ) {
                                Text("퀴즈 화면 즉시 풀기 (97문항)", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = { currentTab = "facts" },
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("연도·법령 연혁 암기장 보기 (95건)", color = Color(0xFF90CAF9))
                            }

                            if (wrongCount > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = {
                                        repository.resetAllStats()
                                        statsTick++
                                        Toast.makeText(this@MainActivity, "오답 기록이 초기화되었습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("오답 가중치 기록 초기화", color = Color(0xFFEF9A9A), fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        YearFactsScreen(
                            repository = repository,
                            onBack = {
                                statsTick++
                                currentTab = "home"
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            Toast.makeText(this, "이미 권한이 허용되어 있습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startQuizService() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "먼저 1단계 권한을 허용해 주세요.", Toast.LENGTH_SHORT).show()
            checkOverlayPermission()
            return
        }
        val serviceIntent = Intent(this, LockScreenService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        Toast.makeText(this, "잠금화면 퀴즈가 활성화되었습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun stopQuizService() {
        val serviceIntent = Intent(this, LockScreenService::class.java)
        stopService(serviceIntent)
        Toast.makeText(this, "서비스가 중지되었습니다.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun YearFactsScreen(repository: QuizRepository, onBack: () -> Unit) {
    val factsData = remember { repository.getYearFactsData() }
    val filters = listOf("전체", "★ 핵심빈출", "오답노트", "국제인도법", "규약", "표장규칙", "조직법/정관/규칙", "보충(제외분)")
    var selectedFilter by remember { mutableStateOf("전체") }

    val filteredFacts = remember(selectedFilter, factsData) {
        when (selectedFilter) {
            "전체" -> factsData.facts
            "★ 핵심빈출" -> factsData.facts.filter { it.core }
            "오답노트" -> factsData.facts.filter { repository.getWrongCount(it.id) > 0 }
            "국제인도법" -> factsData.facts.filter { it.subject == "국제인도법" }
            "규약" -> factsData.facts.filter { it.subject == "규약" }
            "표장규칙" -> factsData.facts.filter { it.subject == "표장규칙" }
            "조직법/정관/규칙" -> factsData.facts.filter { it.subject in listOf("조직법", "정관", "시행규칙") }
            else -> emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "연도·법령 연혁 암기장",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onBack) {
                Text("닫기", color = Color(0xFF90CAF9), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filters) { filterName ->
                val isSelected = selectedFilter == filterName
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) Color(0xFF1976D2) else Color(0xFF1E1E1E),
                            RoundedCornerShape(16.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF64B5F6) else Color(0xFF333333),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedFilter = filterName }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = filterName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (selectedFilter == "보충(제외분)") {
                items(factsData.notInQuiz) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = item.yearText,
                                color = Color(0xFFFFD54F),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = item.event, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = item.note, color = Color(0xFFB0BEC5), fontSize = 13.sp)
                            Text(text = "출처: ${item.source}", color = Color(0xFF78909C), fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(filteredFacts) { fact ->
                    val wrongCount = repository.getWrongCount(fact.id)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = fact.yearText,
                                        color = Color(0xFF64B5F6),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "[${fact.subject}]",
                                        color = Color(0xFFB0BEC5),
                                        fontSize = 12.sp
                                    )
                                    if (fact.core) {
                                        Text(
                                            text = "★ 핵심",
                                            color = Color(0xFFFFD54F),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (wrongCount > 0) {
                                    Text(
                                        text = "오답 ${wrongCount}회",
                                        color = Color(0xFFEF9A9A),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = fact.event,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 21.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = fact.explanation,
                                color = Color(0xFFCFD8DC),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "출처: ${fact.source}",
                                color = Color(0xFF78909C),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
