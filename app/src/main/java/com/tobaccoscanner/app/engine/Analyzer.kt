package com.tobaccoscanner.app.engine

import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/*
 * On-device grading engine for Jampuri tobacco photos. A pure Kotlin port of the researcher-grade
 * pipeline from the Jampuri Leaf & Powder Check page:
 *   1. white balance + exposure normalisation from the photo rim and the ROI
 *   2. foreground segmentation with outlier rejection (glare, shadow, grey background, skin, rim)
 *   3. CIE L*a*b* / HSV pixel classification into cured, green, pale stem and defect classes
 *   4. Sobel / Laplacian / multi-scale texture, tile colour spread and delta-E uniformity
 *   5. four mode-specific check scores, an overall score and an A/B/C grade
 * It works on plain ARGB int arrays so it can be unit-tested without Android.
 */

enum class ScanMode { LEAF, POWDER }

/** Scan box in normalised image coordinates (0..1). */
data class Roi(val x: Double, val y: Double, val w: Double, val h: Double) {
    companion object {
        val DEFAULT = Roi(0.20, 0.18, 0.60, 0.64)
    }
}

enum class Lighting { DAYLIGHT, INDOOR, SHADE, LOW_LIGHT, GLARE }
enum class PhotoWarning { NONE, DARK, GLARE, BLURRY, SMALL_AREA }
enum class ColorName { DARK_GOLDEN_BROWN, GREENISH, LIGHT_TAN, DEEP_MAHOGANY }

enum class ReasonId {
    L_CURE_GOOD, L_CURE_LOW, L_OIL_GOOD, L_OIL_LOW, L_GREEN, L_STEM, L_DEFECT, L_CLEAN,
    P_COLOR_GOOD, P_COLOR_LOW, P_STEM_LOW, P_STEM_HIGH, P_GRIT, P_GREEN, P_GRIND_GOOD, P_GRIND_LOW,
}

data class Reason(val id: ReasonId, val value: Int = 0)

class Analysis(
    val mode: ScanMode,
    val grade: Char,
    val visualScore: Int,
    val lighting: Lighting,
    val warning: PhotoWarning,
    val colorArgb: Int,
    val colorName: ColorName,
    val reasons: List<Reason>,
    val m1: Int,
    val m2: Int,
    val m3: Int,
    val m4: Int,
    val pctOptimal: Int,
    val pctGreen: Int,
    val pctPale: Int,
    val pctDark: Int,
    val coveragePct: Int,
    val sharpness: Int,
)

/** [analysis] is null when too little tobacco is inside the scan box. [heatmap] is ARGB, same size as the photo. */
class Outcome(val analysis: Analysis?, val heatmap: IntArray)

object Analyzer {

    private fun jsRound(x: Double): Int = Math.floor(x + 0.5).toInt()
    private fun clampI(v: Int, lo: Int, hi: Int): Int = max(lo, min(hi, v))

    private fun hsv(r: Int, g: Int, b: Int, out: DoubleArray) {
        val rn = r / 255.0
        val gn = g / 255.0
        val bn = b / 255.0
        val mx = max(rn, max(gn, bn))
        val mn = min(rn, min(gn, bn))
        val d = mx - mn
        var h = 0.0
        if (d != 0.0) {
            h = if (mx == rn) ((gn - bn) / d) % 6
            else if (mx == gn) (bn - rn) / d + 2
            else (rn - gn) / d + 4
            h = jsRound(h * 60).toDouble()
            if (h < 0) h += 360.0
        }
        out[0] = h
        out[1] = if (mx == 0.0) 0.0 else (d / mx) * 100
        out[2] = mx * 100
    }

    private fun lab(r: Int, g: Int, b: Int, out: DoubleArray) {
        var rn = r / 255.0
        var gn = g / 255.0
        var bn = b / 255.0
        rn = if (rn > 0.04045) ((rn + 0.055) / 1.055).pow(2.4) else rn / 12.92
        gn = if (gn > 0.04045) ((gn + 0.055) / 1.055).pow(2.4) else gn / 12.92
        bn = if (bn > 0.04045) ((bn + 0.055) / 1.055).pow(2.4) else bn / 12.92

        var x = (rn * 0.4124564 + gn * 0.3575761 + bn * 0.1804375) / 0.95047
        var y = (rn * 0.2126729 + gn * 0.7151522 + bn * 0.0721750) / 1.00000
        var z = (rn * 0.0193339 + gn * 0.1191920 + bn * 0.9503041) / 1.08883

        val eps = 0.008856
        val kappa = 903.3
        x = if (x > eps) Math.cbrt(x) else (kappa * x + 16) / 116
        y = if (y > eps) Math.cbrt(y) else (kappa * y + 16) / 116
        z = if (z > eps) Math.cbrt(z) else (kappa * z + 16) / 116

        val l = 116 * y - 16
        val a = 500 * (x - y)
        val bv = 200 * (y - z)
        out[0] = l
        out[1] = a
        out[2] = bv
        out[3] = hypot(a, bv)
    }

    private fun percentile(arr: DoubleArray, n: Int, p: Int): Double {
        if (n == 0) return 0.0
        val sorted = arr.copyOf(n)
        sorted.sort()
        val idx = min(n - 1, max(0, Math.floor(p / 100.0 * n).toInt()))
        return sorted[idx]
    }

    private fun hm(a: Int, r: Int, g: Int, b: Int): Int = (a shl 24) or (r shl 16) or (g shl 8) or b

    fun analyze(pixels: IntArray, w: Int, h: Int, roi: Roi, mode: ScanMode): Outcome {
        val heat = IntArray(w * h)

        val x0 = max(2, Math.floor(roi.x * w).toInt())
        val y0 = max(2, Math.floor(roi.y * h).toInt())
        val x1 = min(w - 3, Math.floor((roi.x + roi.w) * w).toInt())
        val y1 = min(h - 3, Math.floor((roi.y + roi.h) * h).toInt())
        val roiW = max(1, x1 - x0)
        val roiH = max(1, y1 - y0)

        // 1. Illumination and white-balance normalisation
        var rimR = 0.0
        var rimG = 0.0
        var rimB = 0.0
        var rimCount = 0
        var neutralR = 0.0
        var neutralG = 0.0
        var neutralB = 0.0
        var neutralCount = 0
        val bStep = max(3, Math.floor(min(w, h) / 85.0).toInt())

        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                if (x < w * 0.06 || x > w * 0.94 || y < h * 0.06 || y > h * 0.94) {
                    val p = pixels[y * w + x]
                    val r = (p shr 16) and 255
                    val g = (p shr 8) and 255
                    val b = p and 255
                    rimR += r; rimG += g; rimB += b
                    rimCount++
                    val maxC = max(r, max(g, b))
                    val minC = min(r, min(g, b))
                    if (maxC > 60 && maxC < 245 && (maxC - minC).toDouble() / maxC < 0.24) {
                        neutralR += r; neutralG += g; neutralB += b
                        neutralCount++
                    }
                }
                x += bStep
            }
            y += bStep
        }
        rimR = if (rimCount > 0) rimR / rimCount else 128.0
        rimG = if (rimCount > 0) rimG / rimCount else 128.0
        rimB = if (rimCount > 0) rimB / rimCount else 128.0

        val lumaCap = ((roiH + 1) / 2 + 1) * ((roiW + 1) / 2 + 1)
        val roiLumas = DoubleArray(lumaCap)
        var lumaN = 0
        var roiR = 0.0
        var roiG = 0.0
        var roiB = 0.0
        var roiSampleCount = 0
        var rawGlareCount = 0
        y = y0
        while (y < y1) {
            var x = x0
            while (x < x1) {
                val p = pixels[y * w + x]
                val r = (p shr 16) and 255
                val g = (p shr 8) and 255
                val b = p and 255
                val luma = 0.299 * r + 0.587 * g + 0.114 * b
                roiLumas[lumaN++] = luma
                if (luma > 242) rawGlareCount++
                roiR += r; roiG += g; roiB += b
                roiSampleCount++
                x += 2
            }
            y += 2
        }

        val p50Luma = percentile(roiLumas, lumaN, 50)
        val p95Luma = percentile(roiLumas, lumaN, 95)
        val rawGlareRatio = if (roiSampleCount > 0) rawGlareCount.toDouble() / roiSampleCount else 0.0

        var wbR = 1.0
        var wbG = 1.0
        var wbB = 1.0
        var lighting = Lighting.DAYLIGHT
        var warning = PhotoWarning.NONE

        if (neutralCount >= 12) {
            val nR = neutralR / neutralCount
            val nG = neutralG / neutralCount
            val nB = neutralB / neutralCount
            val nAvg = (nR + nG + nB) / 3
            wbR = max(0.78, min(1.28, nAvg / max(1.0, nR)))
            wbG = max(0.82, min(1.22, nAvg / max(1.0, nG)))
            wbB = max(0.78, min(1.35, nAvg / max(1.0, nB)))
            if (nR / max(1.0, nB) > 1.22) {
                lighting = Lighting.INDOOR
            } else if (nB / max(1.0, nR) > 1.18) {
                lighting = Lighting.SHADE
            }
        } else {
            val mR = roiR / max(1, roiSampleCount)
            val mG = roiG / max(1, roiSampleCount)
            val mB = roiB / max(1, roiSampleCount)
            val mAvg = (mR + mG + mB) / 3
            wbR = 1 + ((mAvg / max(1.0, mR)) - 1) * 0.25
            wbG = 1 + ((mAvg / max(1.0, mG)) - 1) * 0.25
            wbB = 1 + ((mAvg / max(1.0, mB)) - 1) * 0.25
        }

        var gamma = 1.0
        var exposureGain = 1.0
        if (p50Luma < 72) {
            gamma = max(0.68, p50Luma / 96)
            exposureGain = min(1.45, 102 / max(38.0, p50Luma))
            lighting = Lighting.LOW_LIGHT
            warning = PhotoWarning.DARK
        } else if (rawGlareRatio > 0.14 || p95Luma > 238) {
            exposureGain = 0.88
            lighting = Lighting.GLARE
            warning = PhotoWarning.GLARE
        }

        // 2. Foreground segmentation and outlier rejection
        val total = roiW * roiH
        val bufR = IntArray(total)
        val bufG = IntArray(total)
        val bufB = IntArray(total)
        val bufL = FloatArray(total)
        val bufA = FloatArray(total)
        val bufBs = FloatArray(total)
        val bufC = FloatArray(total)
        val bufHue = FloatArray(total)
        val fg = BooleanArray(total)
        var validCount = 0
        val hv = DoubleArray(3)
        val lb = DoubleArray(4)

        for (ry in 0 until roiH) {
            val yy = y0 + ry
            for (rx in 0 until roiW) {
                val xx = x0 + rx
                val pIdx = ry * roiW + rx
                val p = pixels[yy * w + xx]
                val r0 = (p shr 16) and 255
                val g0 = (p shr 8) and 255
                val b0 = p and 255

                var rn = min(255.0, max(0.0, r0 * wbR * exposureGain))
                var gn = min(255.0, max(0.0, g0 * wbG * exposureGain))
                var bn = min(255.0, max(0.0, b0 * wbB * exposureGain))
                if (gamma != 1.0) {
                    rn = 255 * (rn / 255).pow(gamma)
                    gn = 255 * (gn / 255).pow(gamma)
                    bn = 255 * (bn / 255).pow(gamma)
                }
                val r = jsRound(rn)
                val g = jsRound(gn)
                val b = jsRound(bn)
                bufR[pIdx] = r; bufG[pIdx] = g; bufB[pIdx] = b

                lab(r, g, b, lb)
                hsv(r, g, b, hv)
                bufL[pIdx] = lb[0].toFloat()
                bufA[pIdx] = lb[1].toFloat()
                bufBs[pIdx] = lb[2].toFloat()
                bufC[pIdx] = lb[3].toFloat()
                bufHue[pIdx] = hv[0].toFloat()

                val L = lb[0]
                val a = lb[1]
                val bb = lb[2]
                val c = lb[3]
                val isGlare = L > 90 && c < 14
                val isShadow = L < 10
                val isAchromaticBg = c < 9 && L >= 24
                val isNonBotanicalHue = hv[0] > 175 && hv[0] < 330 && hv[1] > 20
                val isSkinTone = L > 60 && L < 86 && a > 12 && bb > 8 && bb < 24 && (a / max(1.0, bb)) > 0.68
                val dr = r0 - rimR
                val dg = g0 - rimG
                val db = b0 - rimB
                val distRim = sqrt(dr * dr + dg * dg + db * db)
                val ndc = hypot((rx - roiW / 2.0) / (roiW / 2.0), (ry - roiH / 2.0) / (roiH / 2.0))
                val isRimBg = ndc > 0.78 && distRim < 22 && c < 16

                if (isGlare || isShadow || isAchromaticBg || isNonBotanicalHue || isSkinTone || isRimBg) {
                    fg[pIdx] = false
                    heat[yy * w + xx] = hm(140, 16, 20, 28)
                } else {
                    fg[pIdx] = true
                    validCount++
                }
            }
        }

        val coveragePct = jsRound(validCount.toDouble() / max(1, total) * 100)
        if (validCount < 140) return Outcome(null, heat)

        // 3. Spatial filters (Sobel ridge, local contrast, Laplacian sharpness)
        val fgL = DoubleArray(validCount)
        val fgA = DoubleArray(validCount)
        val fgB = DoubleArray(validCount)
        val fgC = DoubleArray(validCount)
        var fi = 0
        var sumR = 0.0
        var sumG = 0.0
        var sumB = 0.0
        for (i in 0 until total) {
            if (fg[i]) {
                fgL[fi] = bufL[i].toDouble()
                fgA[fi] = bufA[i].toDouble()
                fgB[fi] = bufBs[i].toDouble()
                fgC[fi] = bufC[i].toDouble()
                fi++
                sumR += bufR[i]; sumG += bufG[i]; sumB += bufB[i]
            }
        }
        val medianL = percentile(fgL, validCount, 50)
        val medianA = percentile(fgA, validCount, 50)
        val medianB = percentile(fgB, validCount, 50)
        val medianC = percentile(fgC, validCount, 50)

        val sobelMag = FloatArray(total)
        val localVar3 = FloatArray(total)
        var lapSum = 0.0
        var interiorFg = 0

        for (ry in 1 until roiH - 1) {
            for (rx in 1 until roiW - 1) {
                val pIdx = ry * roiW + rx
                if (!fg[pIdx]) continue
                val n00 = pIdx - roiW - 1
                val n01 = pIdx - roiW
                val n02 = pIdx - roiW + 1
                val n10 = pIdx - 1
                val n11 = pIdx
                val n12 = pIdx + 1
                val n20 = pIdx + roiW - 1
                val n21 = pIdx + roiW
                val n22 = pIdx + roiW + 1
                if (!fg[n00] || !fg[n01] || !fg[n02] || !fg[n10] || !fg[n12] || !fg[n20] || !fg[n21] || !fg[n22]) continue

                interiorFg++
                val l00 = bufL[n00].toDouble(); val l01 = bufL[n01].toDouble(); val l02 = bufL[n02].toDouble()
                val l10 = bufL[n10].toDouble(); val l11 = bufL[n11].toDouble(); val l12 = bufL[n12].toDouble()
                val l20 = bufL[n20].toDouble(); val l21 = bufL[n21].toDouble(); val l22 = bufL[n22].toDouble()

                val gx = (l02 + 2 * l12 + l22) - (l00 + 2 * l10 + l20)
                val gy = (l20 + 2 * l21 + l22) - (l00 + 2 * l01 + l02)
                sobelMag[pIdx] = (hypot(gx, gy) * 0.25).toFloat()

                val mean3 = (l00 + l01 + l02 + l10 + l11 + l12 + l20 + l21 + l22) / 9
                localVar3[pIdx] = Math.abs(l11 - mean3).toFloat()

                val lap = Math.abs(4 * l11 - (l01 + l10 + l12 + l21))
                lapSum += lap * lap
            }
        }

        val lapVariance = if (interiorFg > 0) lapSum / interiorFg else 12.0
        val sharpness = max(25, min(99, jsRound(35 + log10(1 + lapVariance) * 34)))

        if (warning == PhotoWarning.NONE && sharpness < 48) {
            warning = PhotoWarning.BLURRY
        } else if (warning == PhotoWarning.NONE && coveragePct < 35) {
            warning = PhotoWarning.SMALL_AREA
        }

        // 4. Pixel classification and colour-map overlay
        var optimalCount = 0
        var raughanCount = 0
        var greenCount = 0
        var veinCount = 0
        var defectCount = 0
        var deltaESqSum = 0.0

        for (ry in 0 until roiH) {
            val yy = y0 + ry
            for (rx in 0 until roiW) {
                val xx = x0 + rx
                val pIdx = ry * roiW + rx
                if (!fg[pIdx]) continue

                val L = bufL[pIdx].toDouble()
                val a = bufA[pIdx].toDouble()
                val b = bufBs[pIdx].toDouble()
                val C = bufC[pIdx].toDouble()
                val hue = bufHue[pIdx].toDouble()
                val sobel = sobelMag[pIdx].toDouble()
                val microC = localVar3[pIdx].toDouble()

                val dL = L - medianL
                val dA = a - medianA
                val dB = b - medianB
                deltaESqSum += dL * dL + dA * dA + dB * dB

                // 0 mahogany, 1 raughan, 2 green, 3 vein/pale, 4 defect
                var cls = 0
                if (mode == ScanMode.LEAF) {
                    cls = if (L < 20) 4
                    else if (a < -1.5 || (hue >= 62 && hue <= 150 && C >= 13)) 2
                    else if ((sobel > 5.2 && L > medianL + 3.5) || (L > 64 && a < 8.5) || (L > medianL + 13 && a < medianA - 2.2)) 3
                    else if (C > 24 && L >= 24 && L <= 58 && (microC > 1.8 || C > medianC + 3.5)) 1
                    else if (a > 3.5 && b > 9 && L >= 20 && L <= 63) 0
                    else 3
                } else {
                    cls = if ((a <= 0.5 && b <= 3.5 && C < 12) || (L > 72 && C < 16) || L < 17) 4
                    else if (a < -1.2 || (hue >= 62 && hue <= 152 && C >= 14)) 2
                    else if ((L > medianL + 12 && a < medianA - 2.5) || (L > 58 && C < 22)) 3
                    else if (C > 22 && L >= 19 && L <= 48 && C >= medianC + 2.5) 1
                    else 0
                }

                val hi = yy * w + xx
                when (cls) {
                    0, 1 -> {
                        if (cls == 1) raughanCount++ else optimalCount++
                        heat[hi] = hm(105, 146, 64, 14)
                    }
                    2 -> { greenCount++; heat[hi] = hm(175, 16, 185, 129) }
                    3 -> { veinCount++; heat[hi] = hm(175, 250, 204, 21) }
                    else -> { defectCount++; heat[hi] = hm(185, 225, 29, 72) }
                }
            }
        }

        // 5. Scores and insights
        val deltaEStd = sqrt(deltaESqSum / max(1, validCount))
        val pctOptimal = jsRound((optimalCount + raughanCount).toDouble() / validCount * 100)
        val pctRaughanOnly = jsRound(raughanCount.toDouble() / validCount * 100)
        val pctGreen = jsRound(greenCount.toDouble() / validCount * 100)
        val pctVeinPale = jsRound(veinCount.toDouble() / validCount * 100)
        val pctDefect = max(0, 100 - (pctOptimal + pctGreen + pctVeinPale))

        var var3Sum = 0.0
        var var7Sum = 0.0
        var multiN = 0
        var ry = 3
        while (ry < roiH - 3) {
            var rx = 3
            while (rx < roiW - 3) {
                val pIdx = ry * roiW + rx
                if (fg[pIdx]) {
                    val cL = bufL[pIdx].toDouble()
                    var3Sum += localVar3[pIdx]
                    val mean7 = (
                        bufL[(ry - 3) * roiW + (rx - 3)].toDouble() +
                            bufL[(ry - 3) * roiW + (rx + 3)].toDouble() +
                            bufL[(ry + 3) * roiW + (rx - 3)].toDouble() +
                            bufL[(ry + 3) * roiW + (rx + 3)].toDouble()
                        ) / 4
                    var7Sum += Math.abs(cL - mean7)
                    multiN++
                }
                rx += 2
            }
            ry += 2
        }
        val meanVar3 = if (multiN > 0) var3Sum / multiN else 2.5
        val meanVar7 = if (multiN > 0) var7Sum / multiN else 3.2

        var tileSpreadSq = 0.0
        var tileCount = 0
        for (ty in 0 until 4) {
            for (tx in 0 until 4) {
                val sx0 = Math.floor(tx / 4.0 * roiW).toInt()
                val sx1 = Math.floor((tx + 1) / 4.0 * roiW).toInt()
                val sy0 = Math.floor(ty / 4.0 * roiH).toInt()
                val sy1 = Math.floor((ty + 1) / 4.0 * roiH).toInt()
                var tL = 0.0
                var tA = 0.0
                var tB = 0.0
                var tCnt = 0
                var tyy = sy0
                while (tyy < sy1) {
                    var txx = sx0
                    while (txx < sx1) {
                        val p = tyy * roiW + txx
                        if (fg[p]) {
                            tL += bufL[p]; tA += bufA[p]; tB += bufBs[p]; tCnt++
                        }
                        txx += 2
                    }
                    tyy += 2
                }
                if (tCnt > 10) {
                    val dl = tL / tCnt - medianL
                    val da = tA / tCnt - medianA
                    val db = tB / tCnt - medianB
                    tileSpreadSq += dl * dl + da * da + db * db
                    tileCount++
                }
            }
        }
        val tileDeltaESpread = if (tileCount > 0) sqrt(tileSpreadSq / tileCount) else deltaEStd

        val m1: Int
        val m2: Int
        val m3: Int
        val m4: Int
        var visualScore: Int
        val reasons = ArrayList<Reason>()

        if (mode == ScanMode.LEAF) {
            val uniformity = clampI(jsRound(104 - deltaEStd * 2.6), 15, 99)
            m1 = clampI(jsRound(uniformity * 0.55 + (100 - pctGreen * 2.4) * 0.45), 12, 99)

            val chromaScore = min(100.0, (medianC / 30) * 85)
            val sheenBonus = min(20.0, pctRaughanOnly * 0.45)
            val dryPenalty = if (medianC < 16) (16 - medianC) * 3.0 else 0.0
            m2 = clampI(jsRound(chromaScore + sheenBonus - dryPenalty), 18, 98)

            m3 = clampI(jsRound(102 - pctVeinPale * 2.1), 15, 99)
            m4 = clampI(jsRound(100 - pctDefect * 2.4), 12, 99)

            visualScore = jsRound(m1 * 0.30 + m2 * 0.28 + m3 * 0.22 + m4 * 0.20)

            reasons.add(if (pctOptimal >= 72) Reason(ReasonId.L_CURE_GOOD, pctOptimal) else Reason(ReasonId.L_CURE_LOW, pctOptimal))
            reasons.add(Reason(if (m2 >= 75) ReasonId.L_OIL_GOOD else ReasonId.L_OIL_LOW))
            if (pctGreen > 8) reasons.add(Reason(ReasonId.L_GREEN, pctGreen))
            else if (pctVeinPale > 15) reasons.add(Reason(ReasonId.L_STEM, pctVeinPale))
            else if (pctDefect > 7) reasons.add(Reason(ReasonId.L_DEFECT, pctDefect))
            else reasons.add(Reason(ReasonId.L_CLEAN, pctVeinPale))
        } else {
            val blendConsistency = clampI(jsRound(104 - tileDeltaESpread * 3.2), 15, 99)
            m1 = clampI(jsRound(pctOptimal * 0.65 + blendConsistency * 0.35), 15, 99)

            val coarseness = meanVar3 * 1.4 + meanVar7 * 1.2
            m2 = clampI(jsRound(104 - max(0.0, coarseness - 4.2) * 3.8), 18, 98)

            m3 = clampI(jsRound(102 - pctVeinPale * 2.3), 12, 99)
            m4 = clampI(jsRound(100 - (pctDefect * 2.8 + pctGreen * 2.0)), 10, 99)

            visualScore = jsRound(m1 * 0.32 + m2 * 0.22 + m3 * 0.26 + m4 * 0.20)

            reasons.add(if (pctOptimal >= 72) Reason(ReasonId.P_COLOR_GOOD, pctOptimal) else Reason(ReasonId.P_COLOR_LOW, pctOptimal))
            reasons.add(if (pctVeinPale <= 11) Reason(ReasonId.P_STEM_LOW, pctVeinPale) else Reason(ReasonId.P_STEM_HIGH, pctVeinPale))
            if (pctDefect > 5) reasons.add(Reason(ReasonId.P_GRIT, pctDefect))
            else if (pctGreen > 8) reasons.add(Reason(ReasonId.P_GREEN, pctGreen))
            else reasons.add(Reason(if (m2 >= 75) ReasonId.P_GRIND_GOOD else ReasonId.P_GRIND_LOW))
        }

        // Grade tiers: A 80-100, B 55-79, C below 55
        var grade = 'B'
        if (visualScore >= 80 && pctGreen <= 9 && pctVeinPale <= 14 && pctDefect <= 7) {
            grade = 'A'
        } else if (visualScore < 55 || pctOptimal < 48 || pctGreen > 18 || pctVeinPale > 26 || pctDefect > 14) {
            grade = 'C'
        } else if (visualScore >= 80) {
            visualScore = 78
            grade = 'B'
        }

        val meanR = jsRound(sumR / validCount)
        val meanG = jsRound(sumG / validCount)
        val meanB = jsRound(sumB / validCount)
        val colorArgb = (0xFF shl 24) or (meanR shl 16) or (meanG shl 8) or meanB

        val colorName = if (pctGreen > 15) ColorName.GREENISH
        else if (medianL > 52) ColorName.LIGHT_TAN
        else if (medianL < 28) ColorName.DEEP_MAHOGANY
        else ColorName.DARK_GOLDEN_BROWN

        return Outcome(
            Analysis(
                mode = mode, grade = grade, visualScore = visualScore,
                lighting = lighting, warning = warning,
                colorArgb = colorArgb, colorName = colorName, reasons = reasons,
                m1 = m1, m2 = m2, m3 = m3, m4 = m4,
                pctOptimal = pctOptimal, pctGreen = pctGreen, pctPale = pctVeinPale, pctDark = pctDefect,
                coveragePct = coveragePct, sharpness = sharpness,
            ),
            heat,
        )
    }
}
