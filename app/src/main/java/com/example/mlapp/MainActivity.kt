package com.example.mlapp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val pickPhotoRequestCode: Int = 665
    val REQUEST_IMAGE_CAPTURE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pGal.setOnClickListener { view ->
            pickImage()
        }
        pCam.setOnClickListener {
            view -> dispatchTakePictureIntent()
        }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item
        val id = item.getItemId()

        if (id == R.id.imgL) {
            val vAct= Intent(this,MainActivity::class.java)
            startActivity(vAct)
        }
        if (id == R.id.txtR) {
            val vAct= Intent(this,TextRecog::class.java)
            startActivity(vAct)
        }
        if (id == R.id.txtObj) {
            val vAct= Intent(this, ObjectDetect::class.java)
            startActivity(vAct)
        }
        return super.onOptionsItemSelected(item)

    }
    private fun pickImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, pickPhotoRequestCode)
    }

    override fun onActivityResult(requestCode: Int,
                                  resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            contentIV.setImageBitmap(imageBitmap)
            processImageTagging(imageBitmap)
        }
       else if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                pickPhotoRequestCode -> {
                    val bitmap = getImageFromData(data)
                    bitmap?.apply {
                        contentIV.setImageBitmap(this)
                        processImageTagging(bitmap)
                    }
                }
            }
        }

        super.onActivityResult(requestCode, resultCode,
            data)
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    private fun getImageFromData(data: Intent?): Bitmap? {
        val selectedImage = data?.data
        return        MediaStore.Images.Media.getBitmap(this.contentResolver,
            selectedImage)
    }

    private fun processImageTagging(bitmap: Bitmap) {
        val visionImg =
            FirebaseVisionImage.fromBitmap(bitmap)
        val labeler =
            FirebaseVision.getInstance().getOnDeviceImageLabeler().
            processImage(visionImg)
                .addOnSuccessListener { tags ->
                    pLabel.text = tags.joinToString(" ") {
                        it.text }
                }
                .addOnFailureListener { ex ->
                    Log.wtf("LAB", ex)
                }
    }
}
