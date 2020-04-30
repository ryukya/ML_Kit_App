package com.example.mlapp

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.objects.FirebaseVisionObject
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions
import kotlinx.android.synthetic.main.activity_object_detect.*

class ObjectDetect : AppCompatActivity() {
    private val pickPhotoRequestCode: Int = 667
    val REQUEST_IMAGE_CAPTURE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_object_detect)

        captureImageFab.setOnClickListener {
                view -> dispatchTakePictureIntent()
        }
        capGal.setOnClickListener { view ->
            pickImage()
        }
    }
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }
    override fun onActivityResult(requestCode: Int,
                                  resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            val image = getCapturedImage(imageBitmap)
            runObjectDetection(image)
        }
        else if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                pickPhotoRequestCode -> {
                    val bitmap = getImageFromData(data)
                    bitmap?.apply {
                        //imageView.setImageBitmap(this)
                        val image = getCapturedImage(bitmap)
                        runObjectDetection(image)
                    }
                }
            }
        }

        super.onActivityResult(requestCode, resultCode,
            data)
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
            val vAct = Intent(this, MainActivity::class.java)
            startActivity(vAct)
        }
        if (id == R.id.txtR) {
            val vAct = Intent(this, TextRecog::class.java)
            startActivity(vAct)
        }
        if (id == R.id.txtObj) {
            val vAct = Intent(this, ObjectDetect::class.java)
            startActivity(vAct)
        }

        return super.onOptionsItemSelected(item)

    }


    private fun pickImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, pickPhotoRequestCode)
    }
    private fun getImageFromData(data: Intent?): Bitmap? {
        val selectedImage = data?.data
        return        MediaStore.Images.Media.getBitmap(this.contentResolver,
            selectedImage)
    }
    private fun runObjectDetection(bitmap: Bitmap) {
        val image = FirebaseVisionImage.fromBitmap(bitmap)

        val options = FirebaseVisionObjectDetectorOptions.Builder()
            .setDetectorMode(FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        val detector = FirebaseVision.getInstance().getOnDeviceObjectDetector(options)

        detector.processImage(image)
            .addOnSuccessListener {
                val drawingView = DrawingView(applicationContext, it)
                drawingView.draw(Canvas(bitmap))
                runOnUiThread {
                    imageView.setImageBitmap(bitmap)
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    baseContext, "Oops, something went wrong!",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun getCapturedImage(bitmap1: Bitmap): Bitmap {
        val srcImage = bitmap1
        val scaleFactor = Math.min(
            srcImage.width / imageView.width.toFloat(),
            srcImage.height / imageView.height.toFloat()
        )

        val deltaWidth = (srcImage.width - imageView.width * scaleFactor).toInt()
        val deltaHeight = (srcImage.height - imageView.height * scaleFactor).toInt()

        val scaledImage = Bitmap.createBitmap(
            srcImage, deltaWidth / 1, deltaHeight / 1,
            srcImage.width - deltaWidth, srcImage.height - deltaHeight
        )
        srcImage.recycle()
        return scaledImage

    }

}




class DrawingView(context: Context, var visionObjects: List<FirebaseVisionObject>) : View(context) {

    companion object {
        // mapping table for category to strings: drawing strings
        val categoryNames: Map<Int, String> = mapOf(
            FirebaseVisionObject.CATEGORY_UNKNOWN to "Unknown",
            FirebaseVisionObject.CATEGORY_HOME_GOOD to "Home Goods",
            FirebaseVisionObject.CATEGORY_FASHION_GOOD to "Fashion Goods",
            FirebaseVisionObject.CATEGORY_FOOD to "Food",
            FirebaseVisionObject.CATEGORY_PLACE to "Place",
            FirebaseVisionObject.CATEGORY_PLANT to "Plant"
        )
    }

    val MAX_FONT_SIZE = 96F

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT

        for (item in visionObjects) {

            pen.color = Color.RED
            pen.strokeWidth = 8F
            pen.style = Paint.Style.STROKE
            val box = item.getBoundingBox()
            canvas.drawRect(box, pen)


            val tags: MutableList<String> = mutableListOf()
            tags.add("Category: ${categoryNames[item.classificationCategory]}")
            if (item.classificationCategory != FirebaseVisionObject.CATEGORY_UNKNOWN) {
                tags.add("Confidence: ${item.classificationConfidence!!.times(100).toInt()}%")
            }

            var tagSize = Rect(0, 0, 0, 0)
            var maxLen = 0
            var index: Int = -1

            for ((idx, tag) in tags.withIndex()) {
                if (maxLen < tag.length) {
                    maxLen = tag.length
                    index = idx
                }
            }


            pen.style = Paint.Style.FILL_AND_STROKE
            pen.color = Color.YELLOW
            pen.strokeWidth = 2F

            pen.textSize = MAX_FONT_SIZE
            pen.getTextBounds(tags[index], 0, tags[index].length, tagSize)
            val fontSize: Float = pen.textSize * box.width() / tagSize.width()


            if (fontSize < pen.textSize) pen.textSize = fontSize

            var margin = (box.width() - tagSize.width()) / 2.0F
            if (margin < 0F) margin = 0F


            for ((idx, txt) in tags.withIndex()) {
                canvas.drawText(
                    txt, box.left + margin,
                    box.top + tagSize.height().times(idx + 1.0F), pen
                )
            }
        }
    }
}
