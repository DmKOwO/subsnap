package com.example.subsnap.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.subsnap.data.model.AnkiCard

@Composable
fun CardEditDialog(
    card: AnkiCard,
    onSave: (AnkiCard) -> Unit,
    onDismiss: () -> Unit
) {
    var targetWord by remember { mutableStateOf(card.targetWord) }
    var transcription by remember { mutableStateOf(card.transcription) }
    var wordTranslation by remember { mutableStateOf(card.wordTranslation) }
    var sentence by remember { mutableStateOf(card.sentence) }
    var sentenceTranslation by remember { mutableStateOf(card.sentenceTranslation) }
    var explanation by remember { mutableStateOf(card.explanation) }
    var clozeSentence by remember { mutableStateOf(card.clozeSentence) }
    var partOfSpeech by remember { mutableStateOf(card.partOfSpeech) }
    var cefrLevel by remember { mutableStateOf(card.cefrLevel) }
    var userNotes by remember { mutableStateOf(card.userNotes) }
    var tagsString by remember { mutableStateOf(card.tags.joinToString(", ")) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "✏️ Редактирование карточки",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = targetWord,
                    onValueChange = { targetWord = it },
                    label = { Text("Ключевое слово / Идиома") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = transcription,
                    onValueChange = { transcription = it },
                    label = { Text("Транскрипция (IPA)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = wordTranslation,
                    onValueChange = { wordTranslation = it },
                    label = { Text("Перевод слова") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // CEFR Level selector
                Text(text = "Уровень сложности (CEFR):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("A1", "A2", "B1", "B2", "C1", "C2").forEach { lvl ->
                        FilterChip(
                            selected = cefrLevel.equals(lvl, ignoreCase = true),
                            onClick = { cefrLevel = if (cefrLevel == lvl) "" else lvl },
                            label = { Text(lvl, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = partOfSpeech,
                    onValueChange = { partOfSpeech = it },
                    label = { Text("Часть речи (Noun, Verb, Idiom...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = sentence,
                    onValueChange = { sentence = it },
                    label = { Text("Оригинальное предложение") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = clozeSentence,
                    onValueChange = { clozeSentence = it },
                    label = { Text("Cloze предложение ([...] пропуск)") },
                    placeholder = { Text("e.g. She couldn't [...] her tears.") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = sentenceTranslation,
                    onValueChange = { sentenceTranslation = it },
                    label = { Text("Перевод предложения") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = explanation,
                    onValueChange = { explanation = it },
                    label = { Text("Объяснение нюансов в контексте") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = userNotes,
                    onValueChange = { userNotes = it },
                    label = { Text("Личные заметки / мнемоника") },
                    placeholder = { Text("Как лучше запомнить это слово...") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = tagsString,
                    onValueChange = { tagsString = it },
                    label = { Text("Теги (через запятую)") },
                    placeholder = { Text("movie, daily, business") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Отмена")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val parsedTags = tagsString.split(",")
                            .map { it.trim().removePrefix("#") }
                            .filter { it.isNotBlank() }

                        val updated = card.copy(
                            targetWord = targetWord.trim(),
                            transcription = transcription.trim(),
                            wordTranslation = wordTranslation.trim(),
                            sentence = sentence.trim(),
                            sentenceTranslation = sentenceTranslation.trim(),
                            explanation = explanation.trim(),
                            clozeSentence = clozeSentence.trim(),
                            partOfSpeech = partOfSpeech.trim(),
                            cefrLevel = cefrLevel.trim().uppercase(),
                            userNotes = userNotes.trim(),
                            tags = parsedTags
                        )
                        onSave(updated)
                    }) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}
