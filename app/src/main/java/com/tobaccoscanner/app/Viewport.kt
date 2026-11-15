package com.tobaccoscanner.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tobaccoscanner.app.engine.Roi
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private class Xf(val ox: Float, val oy: Float, val dw: Float, val dh: Float)

private fun xf(cw: Float, ch: Float, sw: Int, sh: Int, zoom: Float, pan: Offset): Xf {
    val base = min(cw / sw, ch / sh)
    val s = base * zoom
    val dw = sw * s
    val dh = sh * s
    return Xf((cw - dw) / 2f + pan.x, (ch - dh) / 2f + pan.y, dw, dh)
}

private const val MODE_NONE = 0
private const val MODE_MOVE = 1
private const val MODE_RESIZE = 2
private const val MODE_PAN = 3

/**
 * Inspection viewport: the photo, an optional colour-map overlay and a draggable / resizable scan box.
 * Drag inside the box to move it, drag the round handle to resize it, drag outside it to pan a zoomed photo.
 */
@Composable
fun Viewport(
    source: ImageBitmap?,
    heatmap: ImageBitmap?,
    showHeat: Boolean,
    roi: Roi,
    zoom: Float,
    pan: Offset,
    onRoi: (Roi) -> Unit,
    onPan: (Offset) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
    empty: @Composable () -> Unit = {},
) {
    var box by remember { mutableStateOf(IntSize.Zero) }
    val roiNow by rememberUpdatedState(roi)
    val panNow by rememberUpdatedState(pan)
    val zoomNow by rememberUpdatedState(zoom)
    val srcNow by rememberUpdatedState(source)

    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .background(Color(0xFF10141C))
            .onSizeChanged { box = it },
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(source != null) {
                    var mode = MODE_NONE
                    var total = Offset.Zero
                    var roiStart = roiNow
                    var panStart = panNow
                    detectDragGestures(
                        onDragStart = { pt ->
                            val src = srcNow
                            if (src == null) {
                                mode = MODE_NONE
                            } else {
                                val t = xf(size.width.toFloat(), size.height.toFloat(), src.width, src.height, zoomNow, panNow)
                                val r = roiNow
                                val rx = t.ox + r.x.toFloat() * t.dw
                                val ry = t.oy + r.y.toFloat() * t.dh
                                val rw = r.w.toFloat() * t.dw
                                val rh = r.h.toFloat() * t.dh
                                val handle = hypot(pt.x - (rx + rw), pt.y - (ry + rh))
                                mode = if (handle <= 30.dp.toPx()) MODE_RESIZE
                                else if (pt.x in rx..(rx + rw) && pt.y in ry..(ry + rh)) MODE_MOVE
                                else MODE_PAN
                                total = Offset.Zero
                                roiStart = r
                                panStart = panNow
                            }
                        },
                        onDrag = { change, amount ->
                            val src = srcNow
                            if (src != null && mode != MODE_NONE) {
                                change.consume()
                                total += amount
                                val t = xf(size.width.toFloat(), size.height.toFloat(), src.width, src.height, zoomNow, panStart)
                                val dx = (total.x / t.dw).toDouble()
                                val dy = (total.y / t.dh).toDouble()
                                when (mode) {
                                    MODE_MOVE -> onRoi(
                                        roiStart.copy(
                                            x = max(0.02, min(0.98 - roiStart.w, roiStart.x + dx)),
                                            y = max(0.02, min(0.98 - roiStart.h, roiStart.y + dy)),
                                        ),
                                    )
                                    MODE_RESIZE -> onRoi(
                                        roiStart.copy(
                                            w = max(0.14, min(0.96 - roiStart.x, roiStart.w + dx)),
                                            h = max(0.14, min(0.96 - roiStart.y, roiStart.h + dy)),
                                        ),
                                    )
                                    MODE_PAN -> onPan(panStart + total)
                                }
                            }
                        },
                        onDragEnd = {
                            if (mode == MODE_MOVE || mode == MODE_RESIZE) onCommit()
                            mode = MODE_NONE
                        },
                        onDragCancel = { mode = MODE_NONE },
                    )
                },
        ) {
            val src = source ?: return@Canvas
            val t = xf(size.width, size.height, src.width, src.height, zoom, pan)
            drawLayer(src, t)
            if (showHeat && heatmap != null) drawLayer(heatmap, t)
            drawScanBox(roi, t)
        }
        if (source == null) empty()
    }
}

private fun DrawScope.drawLayer(img: ImageBitmap, t: Xf) {
    drawImage(
        image = img,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(img.width, img.height),
        dstOffset = IntOffset(t.ox.roundToInt(), t.oy.roundToInt()),
        dstSize = IntSize(t.dw.roundToInt(), t.dh.roundToInt()),
        filterQuality = FilterQuality.Medium,
    )
}

private fun DrawScope.drawScanBox(roi: Roi, t: Xf) {
    val rx = t.ox + roi.x.toFloat() * t.dw
    val ry = t.oy + roi.y.toFloat() * t.dh
    val rw = roi.w.toFloat() * t.dw
    val rh = roi.h.toFloat() * t.dh

    val dim = Path().apply {
        fillType = PathFillType.EvenOdd
        addRect(Rect(t.ox, t.oy, t.ox + t.dw, t.oy + t.dh))
        addRect(Rect(rx, ry, rx + rw, ry + rh))
    }
    drawPath(dim, Color(0x5710141C))

    drawRect(
        color = Color(0xD9FFFFFF),
        topLeft = Offset(rx, ry),
        size = Size(rw, rh),
        style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 15f))),
    )

    val b = min(22.dp.toPx(), min(rw * 0.25f, rh * 0.25f))
    val c = Color(0xFFFFDDB3)
    val sw = 4.dp.toPx()
    fun seg(x1: Float, y1: Float, x2: Float, y2: Float) =
        drawLine(c, Offset(x1, y1), Offset(x2, y2), strokeWidth = sw, cap = StrokeCap.Round)
    seg(rx, ry + b, rx, ry); seg(rx, ry, rx + b, ry)
    seg(rx + rw - b, ry, rx + rw, ry); seg(rx + rw, ry, rx + rw, ry + b)
    seg(rx, ry + rh - b, rx, ry + rh); seg(rx, ry + rh, rx + b, ry + rh)

    val hc = Offset(rx + rw, ry + rh)
    drawCircle(Color(0xFF1A56DB), radius = 11.dp.toPx(), center = hc)
    drawCircle(Color.White, radius = 11.dp.toPx(), center = hc, style = Stroke(width = 2.5.dp.toPx()))
}
