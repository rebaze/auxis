package com.rebaze.auxis.indexer.impl

data class GitSourceConfig (val name: String? = null, val url: String? = null, val enabled: Boolean = false) {
    val isEnabled get() = enabled
}

data class GitSeedConfig (val sources: List<GitSourceConfig>? = null)
