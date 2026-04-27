package com.hermes.mdnotes.widget

import android.app.Activity
import android.os.Bundle
import com.hermes.mdnotes.widget.NotesWidgetReceiver

/**
 * 透明 Activity，仅用于 Widget 刷新按钮回调
 * 启动后立即触发 Widget 更新并 finish，用户无感知
 */
class WidgetRefreshActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotesWidgetReceiver.triggerUpdate(this)
        finish()
    }
}
