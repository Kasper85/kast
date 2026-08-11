package com.kastlg.app.domain.usecases

import com.kastlg.app.data.remote.flixcorn.FlixcornResult
import com.kastlg.app.data.remote.flixcorn.FlixcornSeriesDetail
import com.kastlg.app.domain.repositories.FlixcornRepository

class GetFlixcornSeriesDetail(
    private val repository: FlixcornRepository,
) {
    suspend operator fun invoke(slug: String): FlixcornResult<FlixcornSeriesDetail> {
        return repository.getSeriesDetail(slug)
    }
}
