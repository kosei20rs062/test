package jp.ac.kyusanu.videouploader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import jp.ac.kyusanu.videouploader.databinding.ActivityMainBinding
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Locale



class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    private var resultLauncher = registerForActivityResult(
        StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val resultData = result.data
            resultData?.let { openVideo(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        //setContentView(R.layout.activity_main)

        //val button: Button = findViewById(R.id.button)
        binding.button.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "video/mp4"
            resultLauncher.launch(intent)
        }
    }

    private fun openVideo(resultData: Intent) {
        try {
            val uri: Uri? = resultData.data
            // Uriを表示
            binding.textView.text = String.format(Locale.US, "URL: %s", uri.toString())

            uri?.let {
                // ファイルの実際のパスを取得
                val filePath = getPathFromUri(it)

                // ファイルのバイトデータを読み込む
                val fileBytes = readFileBytes(filePath)
                //デバッグ用
                Log.d("FileBytes", "FileBytes size: ${fileBytes.size}")


                // ここでファイルのバイトデータを使ってサーバーに送信などの処理を行う
                // sendFileToServer(fileBytes) などの関数を呼び出す
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getPathFromUri(uri: Uri): String {
        val contentResolver = applicationContext.contentResolver
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                if (columnIndex != -1) {
                    val displayName = it.getString(columnIndex)
                    val documentFile = DocumentFile.fromSingleUri(applicationContext, uri)
                    if (documentFile != null && documentFile.exists()) {
                        val file = java.io.File(applicationContext.cacheDir, displayName)
                        val inputStream = contentResolver.openInputStream(uri)
                        val outputStream = file.outputStream()
                        inputStream?.use { input ->
                            outputStream.use { output ->
                                input.copyTo(output)
                            }
                        }
                        return file.absolutePath
                    }
                }
            }
        }
        throw IOException("Failed to retrieve path from Uri.")
    }


    private fun readFileBytes(filePath: String): ByteArray {
        val file = java.io.File(filePath)
        val inputStream = BufferedInputStream(file.inputStream())
        val byteArrayOutputStream = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var bytesRead: Int
        try {
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            inputStream.close()
        }
        return byteArrayOutputStream.toByteArray()
    }

}