package com.daniel.paintapp

import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.daniel.paintapp.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.util.UUID


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        fullScreen()

        binding.exportImageBtn.setOnClickListener {
            if (!binding.canvasView.isPaint){
                Toast.makeText(this, "belom ada yg digambar", Toast.LENGTH_SHORT).show()
            }else{

                exportCanvas()

            }
        }

        binding.clearBtn.setOnClickListener {
            binding.canvasView.clearCanvas()
        }
    }

    private fun exportCanvas() {
        val storagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val appName = getString(R.string.app_name)
        val childTarget = "/${appName}/sign"
        val targetFile = File(storagePath, childTarget)

        if (!targetFile.isDirectory) if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Files.createDirectories(targetFile.toPath())
        }

        val fileName = UUID.randomUUID().toString()
        val file = File(targetFile, "${fileName}.jpeg")


        binding.canvasView.cropImage().also {
            var out: OutputStream? = null
            try {
                file.createNewFile()
                out = FileOutputStream(file)
                it.compress(Bitmap.CompressFormat.JPEG, 100, out)
            } finally {
                Toast.makeText(this, "Finish export", Toast.LENGTH_SHORT).show()
                out?.close()
            }
        }
    }

    private fun fullScreen() {
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        // Configure the behavior of the hidden system bars.
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Add a listener to update the behavior of the toggle fullscreen button when
        // the system bars are hidden or revealed.
        window.decorView.setOnApplyWindowInsetsListener { view, windowInsets ->
            // You can hide the caption bar even when the other system bars are visible.
            // To account for this, explicitly check the visibility of navigationBars()
            // and statusBars() rather than checking the visibility of systemBars().
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            view.onApplyWindowInsets(windowInsets)
        }
    }
}