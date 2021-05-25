package com.example.myapplication.response

import com.google.gson.annotations.SerializedName

data class PostImageResponse(

	@field:SerializedName("saved")
	val saved: Boolean,

	@field:SerializedName("list_upload")
	val listUpload: ListUpload,

	@field:SerializedName("recent_upload")
	val recentUpload: String
)
