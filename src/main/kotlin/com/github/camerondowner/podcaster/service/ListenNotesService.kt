package com.github.camerondowner.podcaster.service

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.github.camerondowner.podcaster.controller.Episode
import com.github.camerondowner.podcaster.controller.Podcast
import kotlin.math.roundToInt
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.util.Random


@Service
class ListenNotesService(
    webClientBuilder: WebClient.Builder,
    @Value("\${listennotes.apikey}") apiKey: String
) {

    private val webClient = webClientBuilder.baseUrl("https://listen-api.listennotes.com/api/v2")
        .defaultHeader("X-ListenAPI-Key", apiKey)
        .build()

    @Cacheable("findPodcast")
    fun findPodcastByNameAsync(podcastName: String): Deferred<Podcast?> {
        return GlobalScope.async {
            webClient.get()
                .uri {
                    it.path("/search")
                        .queryParam("type", "podcast")
                        .queryParam("only_in", "title")
                        .queryParam("language", "English")
                        .queryParam("q", podcastName)
                        .build()
                }
                .retrieve()
                .awaitBody<SearchPodcastResponse>()
                .results
                .map {
                    it.toListenNotesPodcast()
                }
                .map {
                    Podcast(
                        title = it.title,
                        description = it.description,
                        thumbnail = it.thumbnail,
                        listenNotesId = it.id
                    )
                }.firstOrNull()
        }
    }

    @Cacheable("podcastRecommendations")
    fun findPodcastRecommendationsAsync(podcast: Podcast): Deferred<List<Podcast>> {
        return GlobalScope.async {
            webClient.get()
                .uri("/podcasts/{podcastId}/recommendations", podcast.listenNotesId)
                .retrieve()
                .awaitBody<RecommendationsResponse>()
                .recommendations
                .map {
                    Podcast(
                        title = it.title,
                        description = it.description,
                        thumbnail = it.thumbnail,
                        listenNotesId = it.id
                    )
                }
        }
    }

    suspend fun getPodcastWithPreviewEpisode(podcastId: String): Podcast {
        val randomPodcast = getPodcastById(podcastId)
        val randomEpisode = randomPodcast.episodes.randomOrNull()

        val previewEpisode = randomEpisode?.let {
            Episode(
                title = it.title,
                description = it.description,
                audio = it.audio,
                audioLengthSec = it.audioLengthSec,
                previewStartingTimeSec = getRandomStartingTimeSec(it.audioLengthSec)
            )
        }

        return Podcast(
            title = randomPodcast.title,
            description = randomPodcast.description,
            thumbnail = randomPodcast.thumbnail,
            listenNotesId = randomPodcast.id,
            previewEpisode = previewEpisode,
        )
    }

    private val minTimeFromEnd = 45

    private fun getRandomStartingTimeSec(audioLengthSec: Int): Int {
        if (minTimeFromEnd > audioLengthSec) {
            return 0
        }

        val random = Random().nextGaussian()
        val halfWayPoint = (audioLengthSec / 2)
        val startingPoint = halfWayPoint + (random * (audioLengthSec / 3)).roundToInt()

        if (startingPoint > (audioLengthSec - minTimeFromEnd)) {
            return audioLengthSec - minTimeFromEnd
        }

        return startingPoint
    }

    @Cacheable("completePodcast")
    suspend fun getPodcastById(podcastId: String): ListenNotesPodcast {
        println("getPodcastById: $podcastId")
        return webClient.get()
            .uri("/podcasts/{podcastId}", podcastId)
            .retrieve()
            .awaitBody()
    }

    private suspend fun getBestPodcastsInGenre(genreId: Int): BestPodcastResponse {
        return webClient.get()
            .uri {
                it.path("/best_podcasts")
                    .queryParam("genre_id", genreId)
                    .queryParam("page", 1)
                    .queryParam("region", "gb")
                    .build()
            }
            .retrieve()
            .awaitBody()
    }

}

data class BestPodcastResponse(
    val podcasts: List<ListenNotesPodcast>
)

data class SearchPodcastResponse(
    val results: List<ListenNotesPodcastSearchResult>
)

data class RecommendationsResponse(
    val recommendations: List<ListenNotesPodcast>
)

data class ListenNotesPodcast(
    val id: String,
    val title: String,
    val description: String,
    val thumbnail: String,
    val episodes: List<ListenNotesEpisode> = emptyList()
)

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class ListenNotesPodcastSearchResult(
    val id: String,
    val titleOriginal: String,
    val descriptionOriginal: String,
    val thumbnail: String
) {
    fun toListenNotesPodcast(): ListenNotesPodcast {
        return ListenNotesPodcast(
            id, titleOriginal, descriptionOriginal, thumbnail
        )
    }
}

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class ListenNotesEpisode(
    val id: String,
    val title: String,
    val description: String,
    val audio: String,
    val audioLengthSec: Int
)
