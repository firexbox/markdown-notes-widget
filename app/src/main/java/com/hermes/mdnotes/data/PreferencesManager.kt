package com.hermes.mdnotes.data

import android.content.Context
import android.util.Log
import java.io.File

/**
 * 目录持久化管理器
 */
object PreferencesManager {

    private const val TAG = "MdNotesPrefs"
    private const val PREFS_NAME = "mdnotes_prefs"
    private const val KEY_NOTES_DIR = "notes_directory_path"
    private const val KEY_FIRST_LAUNCH = "first_launch_done"

    /**
     * 获取持久化的笔记目录
     * @return 有效的目录，或 null（用户尚未选择）
     */
    fun getNotesDirectory(context: Context): File? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedPath = prefs.getString(KEY_NOTES_DIR, null) ?: return null

        val dir = File(savedPath)
        if (dir.exists() && dir.isDirectory && dir.canWrite()) {
            return dir
        }
        // 目录存在但不可写，尝试修复
        if (dir.exists() && !dir.canWrite()) {
            Log.w(TAG, "Directory exists but not writable: $savedPath")
            return null
        }
        // 目录被删了，尝试重建
        if (dir.mkdirs() && dir.canWrite()) {
            Log.d(TAG, "Recreated dir: $savedPath")
            return dir
        }
        Log.w(TAG, "Saved directory inaccessible: $savedPath")
        return null
    }

    /**
     * 保存目录路径
     */
    fun saveNotesDirectory(context: Context, dir: File) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_NOTES_DIR, dir.absolutePath)
            .putBoolean(KEY_FIRST_LAUNCH, true)
            .apply()
        Log.d(TAG, "Saved notes dir: ${dir.absolutePath}")
    }

    /** 是否首次启动 */
    fun isFirstLaunch(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getBoolean(KEY_FIRST_LAUNCH, false)
    }

    /** 标记首次启动已完成 */
    fun markFirstLaunchDone(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_FIRST_LAUNCH, true).apply()
    }
}
