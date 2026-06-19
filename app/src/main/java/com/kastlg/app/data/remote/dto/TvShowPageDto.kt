package com.kastlg.app.data.remote.dto

data class TvShowPageDto(
    val page: Int,
    val results: List<TvShowDto>,
    @com.google.gson.annotations.SerializedName("total_pages")
    val totalPages: Int,
    @com.google.gson.annotations.SerializedName("total_results")
    val totalResults: Int,
)
