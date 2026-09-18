package com.example.subsnap.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SubtitleBandDiffDetectorTest {

    private lateinit var detector: SubtitleBandDiffDetector

    @Before
    fun setUp() {
        detector = SubtitleBandDiffDetector(
            gridCols = 48,
            gridRows = 16,
            noiseThreshold = 20,
            minChangedFraction = 0.035f,
            minMeanDelta = 3.5f
        )
    }

    @Test
    fun testInitialFrame_establishesBaselineAndDoesNotTrigger() {
        val frame = IntArray(48 * 16) { 30 } // Dark video scene
        val result = detector.compare(frame)

        assertTrue(result.isInitialFrame)
        assertFalse(result.isSignificantChange)
        assertEquals(0f, result.meanLuminanceDelta, 0.001f)
        assertEquals(0f, result.changedFraction, 0.001f)
        assertTrue(detector.hasBaseline)
    }

    @Test
    fun testIdenticalFrames_returnsZeroDiffAndNoTrigger() {
        val frame = IntArray(48 * 16) { 50 }
        detector.compare(frame) // baseline

        val secondResult = detector.compare(frame)
        assertFalse(secondResult.isInitialFrame)
        assertFalse(secondResult.isSignificantChange)
        assertEquals(0f, secondResult.meanLuminanceDelta, 0.001f)
        assertEquals(0f, secondResult.changedFraction, 0.001f)
    }

    @Test
    fun testSubtitleAppearance_triggersSignificantChange() {
        val total = 48 * 16
        val baseline = IntArray(total) { 30 } // Dark scene
        detector.compare(baseline)

        // Subtitle appears: ~8% of sample points become bright white (luma 240)
        val subtitleFrame = baseline.clone()
        val subtitlePixelCount = (total * 0.08).toInt()
        for (i in 0 until subtitlePixelCount) {
            subtitleFrame[i] = 240
        }

        val result = detector.compare(subtitleFrame)
        assertFalse(result.isInitialFrame)
        assertTrue("Expected subtitle appearance to trigger significant change", result.isSignificantChange)
        assertTrue(result.changedFraction >= 0.07f)
        assertTrue(result.meanLuminanceDelta >= 10f)
    }

    @Test
    fun testStaticSubtitle_doesNotTriggerOnceBaselineUpdated() {
        val total = 48 * 16
        val baseline = IntArray(total) { 30 }
        detector.compare(baseline)

        val subtitleFrame = baseline.clone()
        for (i in 0 until (total * 0.08).toInt()) {
            subtitleFrame[i] = 240
        }

        val firstResult = detector.compare(subtitleFrame)
        assertTrue(firstResult.isSignificantChange)

        // Update baseline to the new subtitle frame (as done in production after capture or settle)
        detector.updateBaseline(subtitleFrame)

        // Consecutive frame with the subtitle still on screen
        val secondResult = detector.compare(subtitleFrame)
        assertFalse("Static subtitle must not trigger duplicate diff", secondResult.isSignificantChange)
        assertEquals(0f, secondResult.meanLuminanceDelta, 0.001f)
    }

    @Test
    fun testMinorVideoNoise_doesNotTrigger() {
        val total = 48 * 16
        val baseline = IntArray(total) { 100 }
        detector.compare(baseline)

        // Add minor compression / dithering noise (+/- 5 luma, below noiseThreshold 20)
        val noisyFrame = IntArray(total) { idx ->
            if (idx % 2 == 0) 105 else 95
        }

        val result = detector.compare(noisyFrame)
        assertFalse("Minor video noise below threshold must not trigger", result.isSignificantChange)
        assertEquals(0f, result.changedFraction, 0.001f)
    }

    @Test
    fun testReset_clearsBaseline() {
        val frame = IntArray(48 * 16) { 80 }
        detector.compare(frame)
        assertTrue(detector.hasBaseline)

        detector.reset()
        assertFalse(detector.hasBaseline)

        val nextResult = detector.compare(frame)
        assertTrue(nextResult.isInitialFrame)
        assertFalse(nextResult.isSignificantChange)
    }

    @Test
    fun testMajorSceneTransition_triggersImmediately() {
        val total = 48 * 16
        val darkScene = IntArray(total) { 10 }
        detector.compare(darkScene)

        // Sudden bright cut across the scene
        val brightScene = IntArray(total) { 200 }
        val result = detector.compare(brightScene)

        assertTrue("Major scene brightness transition must be flagged", result.isSignificantChange)
        assertTrue(result.meanLuminanceDelta > 150f)
    }
}
