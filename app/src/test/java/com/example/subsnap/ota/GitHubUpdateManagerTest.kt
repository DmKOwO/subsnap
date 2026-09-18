package com.example.subsnap.ota

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubUpdateManagerTest {

    @Test
    fun testIsNewerVersion_newerMajor() {
        assertTrue(GitHubUpdateManager.isNewerVersion("v2.0.0", "1.0.0"))
    }

    @Test
    fun testIsNewerVersion_newerMinor() {
        assertTrue(GitHubUpdateManager.isNewerVersion("v1.1.0", "1.0.0"))
        assertTrue(GitHubUpdateManager.isNewerVersion("1.1", "1.0"))
    }

    @Test
    fun testIsNewerVersion_newerPatch() {
        assertTrue(GitHubUpdateManager.isNewerVersion("v1.0.1", "1.0"))
    }

    @Test
    fun testIsNewerVersion_sameVersion() {
        assertFalse(GitHubUpdateManager.isNewerVersion("v1.0", "1.0"))
        assertFalse(GitHubUpdateManager.isNewerVersion("1.0.0", "v1.0.0"))
    }

    @Test
    fun testIsNewerVersion_olderVersion() {
        assertFalse(GitHubUpdateManager.isNewerVersion("v0.9.9", "1.0.0"))
        assertFalse(GitHubUpdateManager.isNewerVersion("0.8", "1.0"))
    }

    @Test
    fun testCalculateVersionDiff_withNewerReleases() {
        val releases = listOf(
            AppReleaseRecord(
                tagName = "v1.1.0",
                versionName = "1.1.0",
                releaseName = "SubSnap v1.1.0",
                changelog = "• Added Card Search\n• Added TTS Accent controls",
                publishedAt = "2026-09-18",
                htmlUrl = "https://github.com/DmKOwO/subsnap/releases/tag/v1.1.0",
                apkDownloadUrl = "https://github.com/DmKOwO/subsnap/releases/download/v1.1.0/subsnap.apk",
                apkSizeBytes = 12000000L,
                isCurrent = false,
                isNewer = true
            ),
            AppReleaseRecord(
                tagName = "v1.0.0",
                versionName = "1.0.0",
                releaseName = "SubSnap v1.0.0",
                changelog = "Initial release",
                publishedAt = "2026-09-17",
                htmlUrl = "https://github.com/DmKOwO/subsnap/releases/tag/v1.0.0",
                apkDownloadUrl = "https://github.com/DmKOwO/subsnap/releases/download/v1.0.0/subsnap.apk",
                apkSizeBytes = 11000000L,
                isCurrent = true,
                isNewer = false
            )
        )

        val diff = GitHubUpdateManager.calculateVersionDiff("1.0.0", releases)
        assertTrue(diff.hasUpdate)
        org.junit.Assert.assertEquals(1, diff.newerReleasesCount)
        org.junit.Assert.assertEquals("1.1.0", diff.latestVersion)
        assertTrue(diff.aggregatedChangelog.contains("Added Card Search"))
    }

    @Test
    fun testCalculateVersionDiff_whenCurrentIsLatest() {
        val releases = listOf(
            AppReleaseRecord(
                tagName = "v1.0.0",
                versionName = "1.0.0",
                releaseName = "SubSnap v1.0.0",
                changelog = "Initial release",
                publishedAt = "2026-09-17",
                htmlUrl = "https://github.com/DmKOwO/subsnap/releases/tag/v1.0.0",
                apkDownloadUrl = null,
                apkSizeBytes = 0L,
                isCurrent = true,
                isNewer = false
            )
        )

        val diff = GitHubUpdateManager.calculateVersionDiff("1.0.0", releases)
        assertFalse(diff.hasUpdate)
        org.junit.Assert.assertEquals(0, diff.newerReleasesCount)
        assertTrue(diff.aggregatedChangelog.contains("актуальная версия"))
    }
}
