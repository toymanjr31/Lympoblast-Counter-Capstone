package com.example.myapplication.response

import com.google.gson.annotations.SerializedName

data class ListUpload(

    @field:SerializedName("1")
    val jsonMember1: String,

    @field:SerializedName("2")
    val jsonMember2: String,

    @field:SerializedName("3")
    val jsonMember3: String
)