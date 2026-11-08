package com.mipatrimonio.app.core.backup

object BackupFormat {
    const val EXTENSION = "mpbak"
    const val MIME_TYPE = "application/octet-stream"
    const val SCHEMA_VERSION = 1
}

sealed class BackupResult<out T> {
    data class Success<T>(val data: T) : BackupResult<T>()
    data class Error(val cause: Throwable? = null) : BackupResult<Nothing>()
}
