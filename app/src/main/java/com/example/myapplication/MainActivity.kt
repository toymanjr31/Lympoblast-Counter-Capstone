package com.example.myapplication

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.myapplication.api.ApiService
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.response.PostImageResponse
import com.example.myapplication.response.ResultResponse
import com.example.myapplication.utils.UploadRequestBody
import com.example.myapplication.utils.getFileName
import com.example.myapplication.utils.snackbar
import com.google.firebase.storage.FirebaseStorage
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), UploadRequestBody.UploadCallback {

    private var selectedImageUri: Uri? = null
    private var imageLink: String? = null
    private var resultPath: String? = null
    private lateinit var activityMainBinding: ActivityMainBinding

    lateinit var filePath: Uri

    companion object {
        const val REQUEST_CODE_PICK_IMAGE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        activityMainBinding.progressBar.visibility = View.INVISIBLE

        activityMainBinding.buttonUpload.setOnClickListener {
            openImageChooser()
        }

        activityMainBinding.buttonDetect.setOnClickListener {
            detectImage()
            uploadToFirestore()
        }
    }

    private fun openImageChooser() {
        Intent(Intent.ACTION_PICK).also {
            it.type = "image/*"
            it.setAction(Intent.ACTION_GET_CONTENT)
            startActivityForResult(it, REQUEST_CODE_PICK_IMAGE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_PICK_IMAGE -> {
                    selectedImageUri = data?.data
                    activityMainBinding.imageView.alpha = 1F
                    Glide.with(this)
                        .load(selectedImageUri)
                        .into(activityMainBinding.imageView)
                    uploadImage()
                }
            }
        }
    }

    private fun uploadToFirestore(){
         if(selectedImageUri!=null) {
             val pd = ProgressDialog(this)
             pd.setTitle("Uploading to Firestore")
             pd.show()

             val formatter = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
             val now = Date()
             val fileName = formatter.format(now)

             val imageRef = FirebaseStorage.getInstance().reference.child("images/$fileName")
             imageRef.putFile(selectedImageUri!!)
                 .addOnSuccessListener { p0 ->
                     pd.dismiss()
                     Toast.makeText(
                         applicationContext,
                         "File Uploaded to Firestore",
                         Toast.LENGTH_LONG
                     ).show()
                 }
                 .addOnFailureListener { p0 ->
                     pd.dismiss()
                     Toast.makeText(applicationContext, p0.message, Toast.LENGTH_LONG).show()

                 }
                 .addOnProgressListener { p0 ->
                     val progress = (100.0 * p0.bytesTransferred) / p0.totalByteCount
                     pd.setMessage("Uploaded ${progress.toInt()}%")
                 }
         }
}



    private fun uploadImage() {
        if (selectedImageUri == null) {
            activityMainBinding.layoutRoot.snackbar(getString(R.string.image_null))
            return
        }

        val parcelFileDescriptor =
            contentResolver.openFileDescriptor(selectedImageUri!!, "r", null) ?: return

        val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
        val file = File(cacheDir, contentResolver.getFileName(selectedImageUri!!))
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)

        activityMainBinding.progressBar.visibility = View.VISIBLE
        activityMainBinding.progressBar.progress = 0

        val body = UploadRequestBody(file, "file", this)
        ApiService().uploadImage(
            MultipartBody.Part.createFormData(
                "file",
                file.name,
                body
            )
        ).enqueue(object : Callback<PostImageResponse> {
            override fun onResponse(
                call: Call<PostImageResponse>,
                response: Response<PostImageResponse>
            ) {
                response.body()?.let {
                    activityMainBinding.layoutRoot.snackbar(getString(R.string.upload_success))
                    imageLink = it.recentUpload
                    activityMainBinding.progressBar.progress = 100
                    activityMainBinding.progressBar.visibility = View.INVISIBLE
                    activityMainBinding.textView.text = getString(R.string.text_desc2)
                }
            }
            override fun onFailure(call: Call<PostImageResponse>, t: Throwable) {
                activityMainBinding.layoutRoot.snackbar(t.message!!)
                activityMainBinding.progressBar.progress = 0
            }
        })

    }

    private fun detectImage(){
        if (imageLink == null) {
            activityMainBinding.layoutRoot.snackbar((getString(R.string.image_null)))
            return
        }
        val imgLinkParsed = imageLink!!.split("/").toTypedArray()
        val imgName = imgLinkParsed[3]

        activityMainBinding.textView.text = getString(R.string.detection_process)
        activityMainBinding.progressBar.visibility = View.VISIBLE
        activityMainBinding.progressBar.progress = 0
        ApiService().getResult(
            imgName
        ).enqueue(object : Callback<ResultResponse>{
            override fun onResponse(
                call: Call<ResultResponse>,
                response: Response<ResultResponse>
            ) {
                response.body()?.let {
                    activityMainBinding.layoutRoot.snackbar(getString(R.string.detect_finish))
                    resultPath = it.resultPath
                    Glide.with(this@MainActivity)
                        .load("http://10.0.2.2:8000$resultPath")
                        .into(activityMainBinding.imageView)
                    activityMainBinding.progressBar.progress = 100
                    activityMainBinding.progressBar.visibility = View.INVISIBLE
                    activityMainBinding.textView.text = getString(R.string.complete_detection)
                }
            }

            override fun onFailure(call: Call<ResultResponse>, t: Throwable) {
                activityMainBinding.layoutRoot.snackbar(t.message!!)
            }

        })
    }

    override fun onProgressUpdate(percentage: Int) {
        activityMainBinding.progressBar.progress = percentage
    }
}