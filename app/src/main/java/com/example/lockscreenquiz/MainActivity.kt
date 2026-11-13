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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "승진고시 잠금화면 퀴즈", fontSize = 22.sp)
                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = { checkOverlayPermission() },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("1단계: '다른 앱 위에 표시' 권한 허용")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { startQuizService() },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("2단계: 잠금화면 퀴즈 서비스 시작 (ON)")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { stopQuizService() },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("잠금화면 퀴즈 서비스 중지 (OFF)")
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        TextButton(
                            onClick = {
                                startActivity(Intent(this@MainActivity, QuizActivity::class.java))
                            }
                        ) {
                            Text("퀴즈 화면 즉시 테스트하기")
                        }
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
