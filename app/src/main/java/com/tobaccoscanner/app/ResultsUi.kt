package com.tobaccoscanner.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tobaccoscanner.app.engine.Analysis
import com.tobaccoscanner.app.engine.ColorName
import com.tobaccoscanner.app.engine.Lighting
import com.tobaccoscanner.app.engine.ReasonId
import com.tobaccoscanner.app.engine.ScanMode

private fun reasonRes(id: ReasonId): Int = when (id) {
    ReasonId.L_CURE_GOOD -> R.string.r_l_cure_good
    ReasonId.L_CURE_LOW -> R.string.r_l_cure_low
    ReasonId.L_OIL_GOOD -> R.string.r_l_oil_good
    ReasonId.L_OIL_LOW -> R.string.r_l_oil_low
    ReasonId.L_GREEN -> R.string.r_l_green
    ReasonId.L_STEM -> R.string.r_l_stem
    ReasonId.L_DEFECT -> R.string.r_l_defect
    ReasonId.L_CLEAN -> R.string.r_l_clean
    ReasonId.P_COLOR_GOOD -> R.string.r_p_color_good
    ReasonId.P_COLOR_LOW -> R.string.r_p_color_low
    ReasonId.P_STEM_LOW -> R.string.r_p_stem_low
    ReasonId.P_STEM_HIGH -> R.string.r_p_stem_high
    ReasonId.P_GRIT -> R.string.r_p_grit
    ReasonId.P_GREEN -> R.string.r_p_green
    ReasonId.P_GRIND_GOOD -> R.string.r_p_grind_good
    ReasonId.P_GRIND_LOW -> R.string.r_p_grind_low
}

fun lightingRes(l: Lighting): Int = when (l) {
    Lighting.DAYLIGHT -> R.string.light_daylight
    Lighting.INDOOR -> R.string.light_indoor
    Lighting.SHADE -> R.string.light_shade
    Lighting.LOW_LIGHT -> R.string.light_low
    Lighting.GLARE -> R.string.light_glare
}

fun colorNameRes(c: ColorName): Int = when (c) {
    ColorName.DARK_GOLDEN_BROWN -> R.string.color_dark_golden
    ColorName.GREENISH -> R.string.color_greenish
    ColorName.LIGHT_TAN -> R.string.color_tan
    ColorName.DEEP_MAHOGANY -> R.string.color_mahogany
}

@Composable
fun GradeCard(a: Analysis) {
    val cs = MaterialTheme.colorScheme
    val container: Color
    val onContainer: Color
    val badge: Color
    val onBadge: Color
    val headRes: Int
    val sumRes: Int
    when (a.grade) {
        'A' -> {
            container = cs.tertiaryContainer; onContainer = cs.onTertiaryContainer
            badge = cs.tertiary; onBadge = cs.onTertiary
            headRes = R.string.grade_a_head; sumRes = R.string.grade_a_sum
        }
        'B' -> {
            container = cs.secondaryContainer; onContainer = cs.onSecondaryContainer
            badge = cs.secondary; onBadge = cs.onSecondary
            headRes = R.string.grade_b_head; sumRes = R.string.grade_b_sum
        }
        else -> {
            container = cs.errorContainer; onContainer = cs.onErrorContainer
            badge = cs.error; onBadge = cs.onError
            headRes = R.string.grade_c_head; sumRes = R.string.grade_c_sum
        }
    }
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = onContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(if (a.mode == ScanMode.LEAF) R.string.grade_tag_leaf else R.string.grade_tag_powder),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(stringResource(headRes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(stringResource(sumRes), style = MaterialTheme.typography.bodyMedium)
                }
                Column(
                    Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(badge)
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(a.grade.toString(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = onBadge)
                    Text("${a.visualScore} / 100", style = MaterialTheme.typography.labelMedium, color = onBadge)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(a.colorArgb))
                        .border(1.dp, onContainer.copy(alpha = 0.35f), CircleShape),
                )
                Text(
                    stringResource(R.string.natural_color, stringResource(colorNameRes(a.colorName))),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                Surface(shape = CircleShape, color = cs.surface.copy(alpha = 0.65f), contentColor = cs.onSurface) {
                    Text(
                        stringResource(lightingRes(a.lighting)),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private class Status(val labelRes: Int, val container: Color, val onContainer: Color, val bar: Color)

@Composable
private fun statusFor(score: Int): Status {
    val cs = MaterialTheme.colorScheme
    return when {
        score >= 80 -> Status(R.string.st_excellent, cs.tertiaryContainer, cs.onTertiaryContainer, cs.tertiary)
        score >= 62 -> Status(R.string.st_good, cs.primaryContainer, cs.onPrimaryContainer, cs.primary)
        score >= 45 -> Status(R.string.st_fair, cs.secondaryContainer, cs.onSecondaryContainer, cs.secondary)
        else -> Status(R.string.st_poor, cs.errorContainer, cs.onErrorContainer, cs.error)
    }
}

@Composable
private fun CheckCard(icon: ImageVector, title: String, score: Int, desc: String) {
    val cs = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    val st = statusFor(score)
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface, contentColor = cs.onSurface),
        border = BorderStroke(1.dp, cs.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(cs.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, contentDescription = null, tint = cs.primary) }
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(shape = CircleShape, color = st.container, contentColor = st.onContainer) {
                    Text(
                        stringResource(st.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
            }
            LinearProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = st.bar,
                trackColor = extra.surfaceMid,
            )
            Text(desc, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
fun CheckCards(a: Analysis) {
    val leaf = a.mode == ScanMode.LEAF
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (leaf) {
            CheckCard(Icons.Filled.WbSunny, stringResource(R.string.c1l_title), a.m1, stringResource(R.string.c1l_desc))
            CheckCard(Icons.Filled.WaterDrop, stringResource(R.string.c2l_title), a.m2, stringResource(R.string.c2l_desc))
            CheckCard(Icons.Filled.Eco, stringResource(R.string.c3l_title), a.m3, stringResource(R.string.c3l_desc))
            CheckCard(Icons.Filled.VerifiedUser, stringResource(R.string.c4l_title), a.m4, stringResource(R.string.c4l_desc))
        } else {
            CheckCard(Icons.Filled.Palette, stringResource(R.string.c1p_title), a.m1, stringResource(R.string.c1p_desc))
            CheckCard(Icons.Filled.Grain, stringResource(R.string.c2p_title), a.m2, stringResource(R.string.c2p_desc))
            CheckCard(Icons.Filled.Grass, stringResource(R.string.c3p_title), a.m3, stringResource(R.string.c3p_desc))
            CheckCard(Icons.Filled.VerifiedUser, stringResource(R.string.c4p_title), a.m4, stringResource(R.string.c4p_desc))
        }
    }
}

@Composable
fun WhyCard(a: Analysis) {
    val cs = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface, contentColor = cs.onSurface),
        border = BorderStroke(1.dp, cs.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.why_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            a.reasons.forEach { r ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(extra.surfaceLow)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = cs.primary, modifier = Modifier.size(20.dp))
                    Text(stringResource(reasonRes(r.id), r.value), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun HandChecksCard(
    a: Analysis,
    checks: List<Boolean>,
    onToggle: (Int) -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val titles = listOf(R.string.chk1_title, R.string.chk2_title, R.string.chk3_title)
    val descs = listOf(R.string.chk1_desc, R.string.chk2_desc, R.string.chk3_desc)
    val count = checks.count { it }

    val container: Color
    val onContainer: Color
    val headline: String
    val badgeText: String
    if (count == 0) {
        container = cs.primaryContainer; onContainer = cs.onPrimaryContainer
        headline = stringResource(R.string.verdict_none)
        badgeText = stringResource(R.string.photo_score, a.visualScore)
    } else {
        val phys = Math.round(count / 3.0 * 100).toInt()
        val combined = Math.round(a.visualScore * 0.65 + phys * 0.35).toInt()
        badgeText = stringResource(R.string.final_score, combined)
        if (combined >= 78 && count == 3 && a.grade != 'C') {
            container = cs.tertiaryContainer; onContainer = cs.onTertiaryContainer
            headline = stringResource(R.string.verdict_buy)
        } else if (combined >= 58 && count >= 2) {
            container = cs.secondaryContainer; onContainer = cs.onSecondaryContainer
            headline = stringResource(R.string.verdict_discount)
        } else {
            container = cs.errorContainer; onContainer = cs.onErrorContainer
            headline = stringResource(R.string.verdict_no)
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface, contentColor = cs.onSurface),
        border = BorderStroke(1.dp, cs.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.phys_heading),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Surface(shape = CircleShape, color = cs.primaryContainer, contentColor = cs.onPrimaryContainer) {
                    Text(
                        stringResource(R.string.phys_count, count),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Text(stringResource(R.string.phys_sub), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)

            for (i in 0 until 3) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, if (checks[i]) cs.primary else cs.outlineVariant, RoundedCornerShape(16.dp))
                        .toggleable(value = checks[i], role = Role.Checkbox, onValueChange = { onToggle(i) })
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(checked = checks[i], onCheckedChange = null)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(titles[i]), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(descs[i]), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(container)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = onContainer)
                Text(badgeText, style = MaterialTheme.typography.labelLarge, color = onContainer)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onSave, modifier = Modifier.weight(1f).height(48.dp)) {
                    Text(stringResource(R.string.btn_save), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FilledTonalButton(onClick = onShare, modifier = Modifier.weight(1f).height(48.dp)) {
                    Text(stringResource(R.string.btn_share), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Optional second opinion from Claude on the whole photo. */
sealed interface AiUi {
    data object Idle : AiUi
    data object Loading : AiUi
    data class Done(val result: ScanResult) : AiUi
    data class Failed(val kind: FailKind) : AiUi
}

@Composable
fun AiCard(state: AiUi, hasKey: Boolean, onAsk: () -> Unit, onAddKey: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface, contentColor = cs.onSurface),
        border = BorderStroke(2.dp, cs.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.ai_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (state is AiUi.Done && state.result.found && state.result.score != null) {
                    Surface(shape = CircleShape, color = cs.primaryContainer, contentColor = cs.onPrimaryContainer) {
                        Text(
                            stringResource(R.string.ai_score, state.result.score),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            when (state) {
                is AiUi.Idle -> {
                    Text(
                        stringResource(if (hasKey) R.string.ai_hint else R.string.ai_need_key),
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                    FilledTonalButton(onClick = if (hasKey) onAsk else onAddKey, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(if (hasKey) R.string.ai_ask else R.string.add_key))
                    }
                }
                is AiUi.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                    Text(stringResource(R.string.ai_loading), style = MaterialTheme.typography.bodyMedium)
                }
                is AiUi.Failed -> {
                    Text(stringResource(failTextRes(state.kind)), style = MaterialTheme.typography.bodyMedium, color = cs.error)
                    FilledTonalButton(onClick = if (state.kind == FailKind.INVALID_KEY) onAddKey else onAsk) {
                        Text(stringResource(if (state.kind == FailKind.INVALID_KEY) R.string.settings else R.string.retry))
                    }
                }
                is AiUi.Done -> AiReport(state.result)
            }
        }
    }
}

fun failTextRes(kind: FailKind): Int = when (kind) {
    FailKind.INVALID_KEY -> R.string.err_invalid_key
    FailKind.RATE_LIMIT -> R.string.err_rate
    FailKind.NETWORK -> R.string.err_network
    FailKind.SERVER -> R.string.err_server
    FailKind.PARSE -> R.string.err_parse
    FailKind.OTHER -> R.string.err_other
}

@Composable
private fun AiReport(r: ScanResult) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!r.found) {
            Text(stringResource(R.string.ai_no_tobacco), style = MaterialTheme.typography.bodyMedium)
            if (r.what.isNotBlank()) Text(r.what, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            return@Column
        }
        if (r.what.isNotBlank()) Text(r.what, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        val rows = listOf(
            R.string.row_form to r.form,
            R.string.row_color to r.color,
            R.string.row_curing to r.curing,
            R.string.row_moisture to r.moisture,
            R.string.row_confidence to "${r.confidence}%",
        ).filter { it.second.isNotBlank() }
        rows.forEach { (label, value) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(label), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
        }
        AiList(R.string.ai_defects, r.defects, cs.errorContainer, cs.onErrorContainer)
        AiList(R.string.ai_positives, r.positives, cs.tertiaryContainer, cs.onTertiaryContainer)
        val advice = buildList {
            addAll(r.advice)
            if (r.confidence < 60) {
                add(stringResource(R.string.low_conf))
                addAll(r.photoTips.take(2))
            }
        }
        AiList(R.string.ai_advice, advice, cs.secondaryContainer, cs.onSecondaryContainer)
        Text(stringResource(R.string.ai_note), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
    }
}

@Composable
private fun AiList(titleRes: Int, items: List<String>, container: Color, content: Color) {
    if (items.isEmpty()) return
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(container).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(titleRes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = content)
        items.forEach { Text("\u2022  $it", style = MaterialTheme.typography.bodySmall, color = content) }
    }
}
