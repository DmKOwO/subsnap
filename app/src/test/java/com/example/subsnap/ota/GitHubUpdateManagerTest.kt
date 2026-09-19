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

    @Test
    fun testIsNewerVersion_v120_vs_120() {
        assertFalse(GitHubUpdateManager.isNewerVersion("v1.2.0", "1.2.0"))
        assertFalse(GitHubUpdateManager.isNewerVersion("1.2.0", "v1.2.0"))
        assertFalse(GitHubUpdateManager.isNewerVersion("V1.2.0", "1.2.0"))
        assertFalse(GitHubUpdateManager.isNewerVersion("  v1.2.0  ", " 1.2.0 "))
    }

    @Test
    fun testIsNewerVersion_localAheadOfRemote() {
        // Local is 1.2.0, remote release is v1.0.2 (must NOT report update available!)
        assertFalse(GitHubUpdateManager.isNewerVersion("v1.0.2", "1.2.0"))
        assertFalse(GitHubUpdateManager.isNewerVersion("1.0.1", "1.2.0"))
        assertFalse(GitHubUpdateManager.isNewerVersion("v1.0.0", "1.2.0"))
    }

    @Test
    fun testIsNewerVersion_edgeCases() {
        assertFalse(GitHubUpdateManager.isNewerVersion("", "1.2.0"))
        assertFalse(GitHubUpdateManager.isNewerVersion("v1.2.0", ""))
        assertFalse(GitHubUpdateManager.isNewerVersion(null, "1.2.0"))
        assertFalse(GitHubUpdateManager.isNewerVersion("v1.2.0", null))
        assertFalse(GitHubUpdateManager.isNewerVersion("1.2", "1.2.0"))
        assertFalse(GitHubUpdateManager.isNewerVersion("1.2.0", "1.2"))
        assertFalse(GitHubUpdateManager.isNewerVersion("v1.2.0-rc1", "1.2.0"))
    }

    @Test
    fun testNormalizeVersion() {
        org.junit.Assert.assertEquals("1.2.0", GitHubUpdateManager.normalizeVersion("v1.2.0"))
        org.junit.Assert.assertEquals("1.2.0", GitHubUpdateManager.normalizeVersion("V1.2.0"))
        org.junit.Assert.assertEquals("1.2.0", GitHubUpdateManager.normalizeVersion("  v1.2.0  "))
        org.junit.Assert.assertEquals("1.2.0", GitHubUpdateManager.normalizeVersion("1.2.0"))
        org.junit.Assert.assertEquals("", GitHubUpdateManager.normalizeVersion(""))
        org.junit.Assert.assertEquals("", GitHubUpdateManager.normalizeVersion(null))
    }

    @Test
    fun testCalculateVersionDiff_whenCurrentAheadOfReleases() {
        val releases = listOf(
            AppReleaseRecord(
                tagName = "v1.0.2",
                versionName = "1.0.2",
                releaseName = "SubSnap v1.0.2",
                changelog = "Bug fixes",
                publishedAt = "2026-09-18",
                htmlUrl = "https://github.com/DmKOwO/subsnap/releases/tag/v1.0.2",
                apkDownloadUrl = "https://github.com/DmKOwO/subsnap/releases/download/v1.0.2/subsnap.apk",
                apkSizeBytes = 12000000L,
                isCurrent = false,
                isNewer = false
            ),
            AppReleaseRecord(
                tagName = "v1.0.1",
                versionName = "1.0.1",
                releaseName = "SubSnap v1.0.1",
                changelog = "Initial release",
                publishedAt = "2026-09-17",
                htmlUrl = "https://github.com/DmKOwO/subsnap/releases/tag/v1.0.1",
                apkDownloadUrl = null,
                apkSizeBytes = 0L,
                isCurrent = false,
                isNewer = false
            )
        )

        val diff = GitHubUpdateManager.calculateVersionDiff("1.2.0", releases)
        assertFalse(diff.hasUpdate)
        org.junit.Assert.assertEquals(0, diff.newerReleasesCount)
        org.junit.Assert.assertEquals("1.2.0", diff.latestVersion)
        org.junit.Assert.assertEquals("1.2.0", diff.currentVersion)
        assertTrue(diff.aggregatedChangelog.contains("актуальная версия"))
    }
}
