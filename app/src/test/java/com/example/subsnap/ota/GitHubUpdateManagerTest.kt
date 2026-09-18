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
}
