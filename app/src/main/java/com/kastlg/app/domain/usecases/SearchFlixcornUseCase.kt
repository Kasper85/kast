package com.kastlg.app.domain.usecases

import com.kastlg.app.data.remote.flixcorn.FlixcornResult
import com.kastlg.app.data.remote.flixcorn.FlixcornSearchResult
import com.kastlg.app.domain.repositories.FlixcornRepository

class SearchFlixcornUseCase(
    private val repository: FlixcornRepository,
) {
    suspend operator fun invoke(query: String): FlixcornResult<List<FlixcornSearchResult>> {
        return repository.searchSeries(query)
    }
}
