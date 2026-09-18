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

    /**
     * Estimates word frequency tier inspired by Yomitan / Migaku dictionaries.
     */
    fun getFrequencyTier(word: String, cefrLevel: String = ""): String {
        val lvl = cefrLevel.uppercase().trim()
        if (lvl == "A1") return "Top 1k"
        if (lvl == "A2") return "Top 3k"
        if (lvl == "B1") return "Top 5k"
        if (lvl == "B2") return "Top 8k"
        if (lvl == "C1") return "Top 15k"
        if (lvl == "C2") return "Rare"

        val len = word.trim().length
        return when {
            len <= 4 -> "Top 3k"
            len <= 7 -> "Top 5k"
            len <= 10 -> "Top 8k"
            else -> "Rare"
        }
    }

    /**
     * Smart word boundary search:
     * 1. Exact word with regex word boundaries (\b).
     * 2. Inflected form matching (e.g. target "walk" matching "walking" or "walked").
     * 3. Loose boundary fallback (surrounded by non-letters).
     * Prevents false matches inside unrelated words (e.g. "cat" inside "Education").
     */
    fun findWordRange(sentence: String, targetWord: String): IntRange? {
        val word = targetWord.trim()
        if (word.isBlank() || sentence.isBlank()) return null

        // 1. Exact word boundary
        val exactRegex = Regex("\\b${Regex.escape(word)}\\b", RegexOption.IGNORE_CASE)
        val exactMatch = exactRegex.find(sentence)
        if (exactMatch != null) return exactMatch.range

        // 2. Inflected form (prefix match with word boundary e.g. "walk" -> "walking", "walks", "walked")
        if (word.length >= 3) {
            val inflectedRegex = Regex("\\b${Regex.escape(word)}[a-zA-Z]*\\b", RegexOption.IGNORE_CASE)
            val inflectedMatch = inflectedRegex.find(sentence)
            if (inflectedMatch != null) return inflectedMatch.range
        }

        // 3. Fallback: boundary with non-letter characters or string boundaries
        val looseRegex = Regex("(^|[^a-zA-Z])${Regex.escape(word)}([^a-zA-Z]|$)", RegexOption.IGNORE_CASE)
        val looseMatch = looseRegex.find(sentence)
        if (looseMatch != null) {
            val offset = if (looseMatch.value.startsWith(word, ignoreCase = true)) 0 else 1
            val start = looseMatch.range.first + offset
            return start until (start + word.length)
        }

        return null
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

@Composable
fun FrequencyBadge(
    tier: String,
    modifier: Modifier = Modifier
) {
    if (tier.isBlank()) return
    val isRare = tier.equals("Rare", ignoreCase = true)
    val color = if (isRare) Color(0xFFE11D48) else Color(0xFF6366F1)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = tier,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DuplicateBadge(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFEA580C).copy(alpha = 0.15f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = "В колоде",
            color = Color(0xFFEA580C),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
