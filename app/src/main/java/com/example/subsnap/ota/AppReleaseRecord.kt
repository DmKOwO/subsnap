package com.example.subsnap.ota

data class AppReleaseRecord(
    val tagName: String,
    val versionName: String,
    val releaseName: String,
    val changelog: String,
    val publishedAt: String,
    val htmlUrl: String,
    val apkDownloadUrl: String?,
    val apkSizeBytes: Long,
    val isCurrent: Boolean,
    val isNewer: Boolean
)

data class VersionDiff(
    val currentVersion: String,
    val latestVersion: String,
    val hasUpdate: Boolean,
    val aggregatedChangelog: String,
    val newerReleasesCount: Int
)
