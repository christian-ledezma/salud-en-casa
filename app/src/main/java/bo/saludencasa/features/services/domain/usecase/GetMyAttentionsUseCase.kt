package bo.saludencasa.features.services.domain.usecase

import bo.saludencasa.features.services.domain.model.AttentionSide
import bo.saludencasa.features.services.domain.model.MyAttentionsPageResult
import bo.saludencasa.features.services.domain.model.MyAttentionsResult
import bo.saludencasa.features.services.domain.repository.IServiceRepository

class GetMyAttentionsUseCase(
    private val repository: IServiceRepository,
) {
    // RNF-08: one bounded page; a short one is the end of the list.
    suspend operator fun invoke(
        side: AttentionSide,
        offset: Int,
    ): MyAttentionsPageResult =
        when (val result = repository.getMyAttentions(side = side, offset = offset, limit = PAGE_SIZE)) {
            is MyAttentionsResult.Loaded -> {
                MyAttentionsPageResult.Loaded(
                    attentions = result.attentions,
                    endReached = result.attentions.size < PAGE_SIZE,
                )
            }

            is MyAttentionsResult.Failure -> {
                MyAttentionsPageResult.Failure(result.error)
            }
        }

    companion object {
        const val PAGE_SIZE = 20
    }
}
