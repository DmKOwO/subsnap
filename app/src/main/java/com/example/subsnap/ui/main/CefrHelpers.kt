package com.example.subsnap.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object CefrHelpers {
    fun getCefrColor(level: String): Color {
        return when (level.uppercase().trim()) {
            "A1" -> Color(0xFF10B981) // Emerald Green
            "A2" -> Color(0xFF059669) // Green
            "B1" -> Color(0xFF0284C7) // Sky Blue
            "B2" -> Color(0xFF2563EB) // Royal Blue
            "C1" -> Color(0xFF7C3AED) // Purple
            "C2" -> Color(0xFFD97706) // Amber / Gold
            else -> Color(0xFF6B7280) // Gray
        }
    }

    fun getCefrLabel(level: String): String {
        return when (level.uppercase().trim()) {
            "A1" -> "A1 · Начальный"
            "A2" -> "A2 · Элементарный"
            "B1" -> "B1 · Средний"
            "B2" -> "B2 · Выше среднего"
            "C1" -> "C1 · Продвинутый"
            "C2" -> "C2 · Владение в совершенстве"
            else -> level.ifBlank { "CEFR" }
        }
    }
}

@Composable
fun CefrBadge(
    level: String,
    modifier: Modifier = Modifier
) {
    if (level.isBlank()) return
    val color = CefrHelpers.getCefrColor(level)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = level.uppercase().trim(),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
