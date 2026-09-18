package com.example.subsnap.ota

data class AppUpdateInfo(
    val tagName: String,
    val versionName: String,
    val changelog: String,
    val apkDownloadUrl: String?,
    val htmlUrl: String,
    val publishedAt: String,
    val isUpdateAvailable: Boolean,
    val currentVersion: String
)
