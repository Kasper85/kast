package com.kastlg.app.data.remote.dto

import com.kastlg.app.domain.models.Genre

data class GenreDto(
    val id: Int,
    val name: String,
)

fun GenreDto.toDomain(): Genre = Genre(
    id = id,
    name = name,
)
