package com.tobaccoscanner.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
private fun gradeColors(grade: String): Pair<Color, Color> {
    val cs = MaterialTheme.colorScheme
    return when (grade) {
        "A" -> cs.tertiary to cs.onTertiary
        "B" -> cs.secondary to cs.onSecondary
        else -> cs.error to cs.onError
    }
}

@Composable
fun HistorySection(
    lots: List<Lot>,
    compareIds: List<Long>,
    onToggle: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onClearCompare: () -> Unit,
    onClose: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface, contentColor = cs.onSurface),
        border = BorderStroke(1.dp, cs.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.history_heading),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_close))
                }
            }
            Text(stringResource(R.string.history_sub), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)

            if (lots.isEmpty()) {
                Text(
                    stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                )
                return@Column
            }

            val picked = compareIds.mapNotNull { id -> lots.firstOrNull { it.id == id } }
            if (picked.size == 2) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(extra.surfaceLow).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.compare),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onClearCompare) { Text(stringResource(R.string.clear)) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        picked.forEach { lot -> CompareCard(lot, Modifier.weight(1f)) }
                    }
                }
            }

            lots.forEach { lot ->
                val selected = compareIds.contains(lot.id)
                val (gBg, gFg) = gradeColors(lot.grade)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) cs.primaryContainer.copy(alpha = 0.4f) else cs.surface,
                    ),
                    border = BorderStroke(1.dp, if (selected) cs.primary else cs.outlineVariant),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(shape = CircleShape, color = gBg, contentColor = gFg) {
                                    Text(
                                        lot.grade,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                                    )
                                }
                                Text(
                                    lot.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                stringResource(
                                    R.string.lot_summary,
                                    stringResource(if (lot.leaf) R.string.word_leaf else R.string.word_powder),
                                    lot.score,
                                    lot.pctOptimal,
                                    lot.timestamp,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant,
                            )
                        }
                        if (selected) {
                            Button(onClick = { onToggle(lot.id) }, modifier = Modifier.height(36.dp)) {
                                Text(stringResource(R.string.selected), style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            OutlinedButton(onClick = { onToggle(lot.id) }, modifier = Modifier.height(36.dp)) {
                                Text(stringResource(R.string.compare), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        IconButton(onClick = { onDelete(lot.id) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareCard(lot: Lot, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val (gBg, gFg) = gradeColors(lot.grade)
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        border = BorderStroke(1.dp, cs.outlineVariant),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(lot.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Surface(shape = CircleShape, color = gBg, contentColor = gFg) {
                    Text(
                        stringResource(R.string.grade_word, lot.grade),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                stringResource(if (lot.leaf) R.string.word_leaf else R.string.word_powder) + " \u2022 " + lot.timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
            )
            CompareRow(stringResource(R.string.comp_score), "${lot.score} / 100")
            CompareRow(stringResource(R.string.comp_good), "${lot.pctOptimal}%")
            CompareRow(stringResource(R.string.comp_pale), "${lot.pctPale}%")
            CompareRow(stringResource(R.string.comp_hand), "${lot.handChecks} / 3")
        }
    }
}

@Composable
private fun CompareRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}
