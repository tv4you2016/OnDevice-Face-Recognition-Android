package com.ioline.ithink.ai.UpdateChecker



///------UPDATE
data class VersionResponse(
    val version_code: Int,
    val url: String,
    val version : String
)

sealed class UpdateResult {
    data class UpdateAvailable(val versionCode: Int, val url: String, val version: String  ): UpdateResult()
    object AlreadyUpToDate: UpdateResult()
    object Error: UpdateResult()
}
