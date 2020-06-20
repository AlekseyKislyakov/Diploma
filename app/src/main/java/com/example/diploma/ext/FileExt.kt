package com.example.diploma.ext

import android.app.Activity
import android.content.Context
import com.example.diploma.view.ControlActivity.Companion.FILE_PICKER_REQUEST_CODE
import com.nbsp.materialfilepicker.MaterialFilePicker
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

fun writeToFile(context: Context, content: String, fileName: String) {
    try {
        val wallpaperDirectory = File(context.getExternalFilesDir(null).toString() + "/control/")
        wallpaperDirectory.mkdirs()
        val file = File(wallpaperDirectory.toString() + "/${fileName}")
        if (!file.exists()) {
            file.createNewFile()
        }
        val writer = FileWriter(file)
        writer.append(content)
        writer.flush()
        writer.close()
    } catch (e: IOException) {
        e.toString()
    }
}

fun readFromFile(context: Context, fileName: String): String {
    val sdcard = context.getExternalFilesDir(null).toString() + "/control/"
    val file = File(sdcard, fileName)
    val text = StringBuilder()

    try {
        val br = BufferedReader(FileReader(file))
        var line: String? = ""
        while (br.readLine().also { line = it } != null) {
            text.append(line)
            text.append('\n')
        }
        br.close()
        return text.toString()
    } catch (e: IOException) {
        //You'll need to add proper error handling here
    }
    return ""
}

fun openFilePickerDialog(activity: Activity) {
    MaterialFilePicker()
        .withActivity(activity)
        .withCloseMenu(true)
        .withHiddenFiles(true)
        .withFilterDirectories(false)
        .withTitle("Choose file")
        .withRequestCode(FILE_PICKER_REQUEST_CODE)
        .start()
}