package com.bizspeak.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bizspeak.app.data.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onSave: (AppSettings) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Ollama 服务", style = MaterialTheme.typography.titleMedium)

            var url = settings.ollamaUrl
            var model = settings.ollamaModel
            var accent = settings.accent
            var tts = settings.ttsEnabled
            var speed = settings.voiceSpeed

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Ollama 地址") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text(
                "模拟器默认 http://10.0.2.2:11434；真机请填电脑在局域网中的 IP（如 http://192.168.1.20:11434）",
                style = MaterialTheme.typography.bodySmall,
            )

            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("模型名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text("例如 llama3.1:8b、qwen2.5:7b、gemma2:9b（需已用 ollama pull 拉取）", style = MaterialTheme.typography.bodySmall)

            OutlinedTextField(
                value = accent,
                onValueChange = { accent = it },
                label = { Text("导师口音") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text("可填 American / British / Australian", style = MaterialTheme.typography.bodySmall)

            androidx.compose.material3.Switch(
                checked = tts,
                onCheckedChange = { tts = it },
            )
            Text("自动朗读导师回复（TTS）", style = MaterialTheme.typography.bodyMedium)

            Text("朗读语速：${String.format("%.1f", speed)}x", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = speed,
                onValueChange = { speed = it },
                valueRange = 0.5f..1.5f,
                steps = 4,
            )

            Button(
                onClick = {
                    onSave(
                        settings.copy(
                            ollamaUrl = url.trim().ifBlank { "http://10.0.2.2:11434" },
                            ollamaModel = model.trim().ifBlank { "llama3.1:8b" },
                            accent = accent.trim().ifBlank { "American" },
                            ttsEnabled = tts,
                            voiceSpeed = speed,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("保存")
            }
        }
    }
}
