package com.yoyuzh.cliplink.domain.usecase

import com.yoyuzh.cliplink.domain.repository.ClipboardRepository
import javax.inject.Inject

class GetLocalHistoryUseCase @Inject constructor(
    private val repository: ClipboardRepository
) {
    operator fun invoke(limit: Int = 20) = repository.observeLocalHistory(limit)
}
