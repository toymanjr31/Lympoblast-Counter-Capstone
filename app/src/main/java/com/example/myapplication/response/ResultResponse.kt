package com.example.myapplication.response

import com.google.gson.annotations.SerializedName

data class ResultResponse(

	@field:SerializedName("result_path")
	val resultPath: String
)
