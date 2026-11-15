package com.tobaccoscanner.app

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.tobaccoscanner.app.engine.Analysis
import com.tobaccoscanner.app.engine.Analyzer
import com.tobaccoscanner.app.engine.PhotoWarning
import com.tobaccoscanner.app.engine.Roi
import com.tobaccoscanner.app.engine.ScanMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val KIND_INFO = 0
private const val KIND_WARN = 1
private const val KIND_SUCCESS = 2

private class JobHolder { var job: Job? = null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerApp(lang: String, onToggleLang: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val cs = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    val jobs = remember { JobHolder() }

    var apiKey by remember { mutableStateOf(Prefs.key(ctx)) }
    var model by remember { mutableStateOf(Prefs.model(ctx)) }

    var mode by rememberSaveable { mutableStateOf(ScanMode.LEAF) }
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var pixels by remember { mutableStateOf<IntArray?>(null) }
    var roi by remember { mutableStateOf(Roi.DEFAULT) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var heatOn by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Analysis?>(null) }
    var heatmap by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    var checks by remember { mutableStateOf(listOf(false, false, false)) }
    var ai by remember { mutableStateOf<AiUi>(AiUi.Idle) }

    var lots by remember { mutableStateOf(LotStore.load(ctx)) }
    var compareIds by remember { mutableStateOf(listOf<Long>()) }
    var showHistory by remember { mutableStateOf(false) }
    var showSave by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    var msgRes by remember { mutableStateOf<Int?>(null) }
    var msgArg by remember { mutableStateOf<String?>(null) }
    var msgKind by remember { mutableStateOf(KIND_INFO) }

    fun setMsg(res: Int?, kind: Int = KIND_INFO, arg: String? = null) {
        msgRes = res
        msgKind = kind
        msgArg = arg
    }

    fun analyze() {
        val px = pixels ?: return
        val bmp = source ?: return
        val currentMode = mode
        val currentRoi = roi
        jobs.job?.cancel()
        jobs.job = scope.launch {
            busy = true
            val w = bmp.width
            val h = bmp.height
            val out = withContext(Dispatchers.Default) { Analyzer.analyze(px, w, h, currentRoi, currentMode) }
            val heat = withContext(Dispatchers.Default) { Bitmap.createBitmap(out.heatmap, w, h, Bitmap.Config.ARGB_8888) }
            heatmap = heat
            result = out.analysis
            val a = out.analysis
            when {
                a == null -> setMsg(R.string.no_pixels, KIND_WARN)
                a.warning == PhotoWarning.DARK -> setMsg(R.string.warn_dark, KIND_WARN)
                a.warning == PhotoWarning.GLARE -> setMsg(R.string.warn_glare, KIND_WARN)
                a.warning == PhotoWarning.BLURRY -> setMsg(R.string.warn_blur, KIND_WARN)
                a.warning == PhotoWarning.SMALL_AREA -> setMsg(R.string.warn_small, KIND_WARN)
                else -> setMsg(null)
            }
            busy = false
        }
    }

    fun newImage(bmp: Bitmap) {
        scope.launch {
            val px = withContext(Dispatchers.Default) {
                IntArray(bmp.width * bmp.height).also { bmp.getPixels(it, 0, bmp.width, 0, 0, bmp.width, bmp.height) }
            }
            source = bmp
            pixels = px
            roi = Roi.DEFAULT
            zoom = 1f
            pan = Offset.Zero
            result = null
            heatmap = null
            ai = AiUi.Idle
            checks = listOf(false, false, false)
            showHistory = false
            analyze()
        }
    }

    fun loadUri(uri: Uri) {
        scope.launch {
            val bmp = withContext(Dispatchers.IO) { ImageUtils.load(ctx, uri, 960) }
            if (bmp == null) setMsg(R.string.err_photo, KIND_WARN) else newImage(bmp)
        }
    }

    fun loadSample(variant: Int) {
        scope.launch {
            val bmp = withContext(Dispatchers.Default) { SampleImages.make(mode, variant) }
            newImage(bmp)
        }
    }

    fun changeMode(m: ScanMode) {
        mode = m
        if (source != null) analyze()
    }

    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) loadUri(uri)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = cameraUri
        if (ok && uri != null) loadUri(uri)
    }

    fun openCamera() {
        val dir = File(ctx.cacheDir, "images").apply { mkdirs() }
        val file = File.createTempFile("scan_", ".jpg", dir)
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
        cameraUri = uri
        camera.launch(uri)
    }

    fun openGallery() {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun askAi() {
        val bmp = source ?: return
        ai = AiUi.Loading
        scope.launch {
            ai = try {
                val language = if (lang == "ur") "Urdu (اردو script), as a Pakistani tobacco buyer would understand it" else "English"
                AiUi.Done(ClaudeScanner.scan(bmp, apiKey, model, language))
            } catch (e: ScanException) {
                AiUi.Failed(e.kind)
            }
        }
    }

    fun saveLot(customName: String) {
        val a = result ?: return
        val default = ctx.getString(
            if (a.mode == ScanMode.LEAF) R.string.lot_default_leaf else R.string.lot_default_powder,
            lots.size + 1,
        )
        val ts = SimpleDateFormat("MMM d, HH:mm", Locale.forLanguageTag(lang)).format(Date())
        val lot = Lot(
            id = System.currentTimeMillis(),
            name = customName.ifBlank { default },
            timestamp = ts,
            leaf = a.mode == ScanMode.LEAF,
            grade = a.grade.toString(),
            score = a.visualScore,
            pctOptimal = a.pctOptimal,
            pctPale = a.pctPale,
            handChecks = checks.count { it },
        )
        lots = LotStore.save(ctx, listOf(lot) + lots)
        showSave = false
        setMsg(R.string.saved_toast, KIND_SUCCESS, lot.name)
    }

    fun share() {
        val a = result ?: return
        val type = ctx.getString(if (a.mode == ScanMode.LEAF) R.string.mode_leaf else R.string.mode_powder)
        val name = ctx.getString(R.string.report_default_name)
        val text = ctx.getString(
            R.string.report_text, name, type, a.grade.toString(), a.visualScore,
            a.pctOptimal, a.pctGreen, a.pctPale, a.pctDark, checks.count { it },
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            this.type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, name)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        ctx.startActivity(Intent.createChooser(send, null))
    }

    LaunchedEffect(showHistory) {
        if (showHistory) scroll.animateScrollTo(scroll.maxValue)
    }

    val sourceImage = remember(source) { source?.asImageBitmap() }
    val heatImage = remember(heatmap) { heatmap?.asImageBitmap() }

    Scaffold(
        containerColor = cs.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.surface),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.size(38.dp).clip(CircleShape).background(cs.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.Eco, contentDescription = null, tint = cs.primary) }
                        Column {
                            Text(
                                stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                stringResource(R.string.app_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showHistory = !showHistory }) {
                        BadgedBox(badge = { if (lots.isNotEmpty()) Badge { Text(lots.size.toString()) } }) {
                            Icon(Icons.Filled.Bookmarks, contentDescription = stringResource(R.string.cd_saved))
                        }
                    }
                    FilledTonalButton(
                        onClick = onToggleLang,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Icon(Icons.Filled.Translate, contentDescription = stringResource(R.string.cd_language), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (lang == "ur") "English" else "اردو", style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.cd_help))
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = cs.surface, tonalElevation = 3.dp, shadowElevation = 8.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(onClick = ::openCamera, modifier = Modifier.weight(1f).height(52.dp)) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_camera), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    FilledTonalButton(onClick = ::openGallery, modifier = Modifier.height(52.dp)) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_upload), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val res = msgRes
            if (res != null) {
                BannerCard(
                    text = if (msgArg != null) stringResource(res, msgArg!!) else stringResource(res),
                    kind = msgKind,
                    onClose = { setMsg(null) },
                )
            }

            ModeSwitch(mode, ::changeMode)

            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface, contentColor = cs.onSurface),
                border = BorderStroke(1.dp, cs.outlineVariant),
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().background(extra.surfaceLow).padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(if (source != null) cs.primary else cs.outline))
                        Text(
                            stringResource(R.string.hud_hint),
                            style = MaterialTheme.typography.labelMedium,
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                        )
                        FilterChip(
                            selected = zoom > 1f,
                            enabled = source != null,
                            onClick = {
                                zoom = if (zoom > 1f) 1f else 2f
                                if (zoom == 1f) pan = Offset.Zero
                            },
                            label = { Text(if (zoom > 1f) "2x" else "1x") },
                            leadingIcon = { Icon(Icons.Filled.ZoomIn, contentDescription = stringResource(R.string.cd_zoom), modifier = Modifier.size(18.dp)) },
                        )
                        FilterChip(
                            selected = heatOn,
                            enabled = source != null,
                            onClick = { heatOn = !heatOn },
                            label = { Text(stringResource(R.string.heatmap_label)) },
                            leadingIcon = { Icon(Icons.Filled.Layers, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        )
                    }

                    Viewport(
                        source = sourceImage,
                        heatmap = heatImage,
                        showHeat = heatOn,
                        roi = roi,
                        zoom = zoom,
                        pan = pan,
                        onRoi = { roi = it },
                        onPan = { pan = it },
                        onCommit = { analyze() },
                        empty = {
                            Column(
                                Modifier.fillMaxSize().padding(20.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    if (mode == ScanMode.LEAF) Icons.Filled.Eco else Icons.Filled.Grain,
                                    contentDescription = null,
                                    tint = Color(0xFFFFDDB3),
                                    modifier = Modifier.size(44.dp),
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.empty_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    stringResource(R.string.empty_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD0D5DD),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        },
                    )

                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())

                    if (heatOn && source != null) {
                        Column(
                            Modifier.fillMaxWidth().background(extra.surfaceLow).padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            LegendRow(Color(0xFF92400E), stringResource(R.string.leg_brown))
                            LegendRow(Color(0xFF10B981), stringResource(R.string.leg_green))
                            LegendRow(Color(0xFFFACC15), stringResource(R.string.leg_yellow))
                            LegendRow(Color(0xFFE11D48), stringResource(R.string.leg_red))
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.sample_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        if (source != null) {
                            TextButton(onClick = {
                                roi = Roi.DEFAULT
                                zoom = 1f
                                pan = Offset.Zero
                                analyze()
                            }) { Text(stringResource(R.string.reset_box)) }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AssistChip(onClick = { loadSample(0) }, label = { Text(stringResource(R.string.sample_a)) })
                        AssistChip(onClick = { loadSample(1) }, label = { Text(stringResource(R.string.sample_b)) })
                        AssistChip(onClick = { loadSample(2) }, label = { Text(stringResource(R.string.sample_c)) })
                    }
                }
            }

            val a = result
            if (a != null) {
                GradeCard(a)
                CheckCards(a)
                WhyCard(a)
                HandChecksCard(
                    a = a,
                    checks = checks,
                    onToggle = { i -> checks = checks.mapIndexed { idx, v -> if (idx == i) !v else v } },
                    onSave = { showSave = true },
                    onShare = ::share,
                )
                AiCard(
                    state = ai,
                    hasKey = apiKey.isNotBlank(),
                    onAsk = ::askAi,
                    onAddKey = { showSettings = true },
                )
            }

            if (showHistory) {
                HistorySection(
                    lots = lots,
                    compareIds = compareIds,
                    onToggle = { id ->
                        compareIds = if (compareIds.contains(id)) compareIds - id
                        else (if (compareIds.size >= 2) compareIds.drop(1) else compareIds) + id
                    },
                    onDelete = { id ->
                        lots = LotStore.save(ctx, lots.filter { it.id != id })
                        compareIds = compareIds - id
                    },
                    onClearCompare = { compareIds = emptyList() },
                    onClose = { showHistory = false },
                )
            }
        }
    }

    if (showSave) {
        SaveDialog(
            defaultName = ctx.getString(
                if (mode == ScanMode.LEAF) R.string.lot_default_leaf else R.string.lot_default_powder,
                lots.size + 1,
            ),
            onDismiss = { showSave = false },
            onConfirm = ::saveLot,
        )
    }
    if (showHelp) HelpDialog(onDismiss = { showHelp = false })
    if (showSettings) {
        SettingsDialog(
            initialKey = apiKey,
            initialModel = model,
            onDismiss = { showSettings = false },
            onSave = { k, m ->
                Prefs.saveApi(ctx, k, m)
                apiKey = Prefs.key(ctx)
                model = Prefs.model(ctx)
                showSettings = false
            },
        )
    }
}

@Composable
private fun BannerCard(text: String, kind: Int, onClose: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val container: Color
    val content: Color
    val icon = when (kind) {
        KIND_WARN -> Icons.Filled.WbTwilight
        KIND_SUCCESS -> Icons.Filled.Check
        else -> Icons.Filled.Info
    }
    when (kind) {
        KIND_WARN -> { container = cs.secondaryContainer; content = cs.onSecondaryContainer }
        KIND_SUCCESS -> { container = cs.tertiaryContainer; content = cs.onTertiaryContainer }
        else -> { container = cs.primaryContainer; content = cs.onPrimaryContainer }
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Row(
            Modifier.padding(start = 14.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_close), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ModeSwitch(mode: ScanMode, onMode: (ScanMode) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(extra.surfaceMid)
            .border(1.dp, cs.outlineVariant, CircleShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ModePill(ScanMode.LEAF, mode, Icons.Filled.Eco, R.string.mode_leaf, onMode, Modifier.weight(1f))
        ModePill(ScanMode.POWDER, mode, Icons.Filled.Grain, R.string.mode_powder, onMode, Modifier.weight(1f))
    }
}

@Composable
private fun ModePill(
    me: ScanMode,
    current: ScanMode,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    labelRes: Int,
    onMode: (ScanMode) -> Unit,
    modifier: Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val selected = me == current
    Row(
        modifier
            .clip(CircleShape)
            .background(if (selected) cs.secondaryContainer else Color.Transparent)
            .clickable { onMode(me) }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = cs.onSecondaryContainer, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
        }
        Icon(icon, contentDescription = null, tint = if (selected) cs.onSecondaryContainer else cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(labelRes),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (selected) cs.onSecondaryContainer else cs.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LegendRow(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
