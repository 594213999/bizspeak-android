package com.bizspeak.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bizspeak.app.data.Level
import com.bizspeak.app.data.Scenario
import com.bizspeak.app.data.SettingsStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    level: Level,
    scenario: Scenario,
    onLevelChange: (Level) -> Unit,
    onScenarioChange: (Scenario) -> Unit,
    onStart: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BizSpeak 商务口语陪练") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Text("设置")
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("选择练习水平", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LevelRow(selected = level, onSelect = onLevelChange)

            Text("选择商务场景", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(Scenario.entries.toList()) { s ->
                    ScenarioCard(
                        scenario = s,
                        selected = s == scenario,
                        onClick = { onScenarioChange(s) },
                    )
                }
            }

            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("开始对话 ▶")
            }
        }
    }
}

@Composable
private fun LevelRow(selected: Level, onSelect: (Level) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Level.entries.forEach { lvl ->
            val isSel = lvl == selected
            Card(
                onClick = { onSelect(lvl) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                ) {
                    Text(
                        lvl.label,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScenarioCard(scenario: Scenario, selected: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                scenario.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(scenario.description, style = MaterialTheme.typography.bodySmall)
        }
    }
}
