package com.vega.core.context

// keepDuration: 维持时间，单位 millisecond
data class LaunchMode(val mode: String, val keepDuration: Long = 0L) {

    companion object {
        val LAUNCH = LaunchMode("launch") // 手动拉起
        val AWEME_EDIT = LaunchMode("aweme_edit", 30 * 60 * 1000L) // 工具锚点
        val AWEME_TEMPLATE = LaunchMode("aweme_template", 30 * 60 * 1000L) // 工具锚点,
        val OTHER = LaunchMode("other") // 其它方式
    }
}
