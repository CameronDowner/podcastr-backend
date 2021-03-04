package com.github.camerondowner.podcaster.service

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.github.camerondowner.podcaster.controller.Episode
import com.github.camerondowner.podcaster.controller.Podcast
import kotlin.math.roundToInt
import org.springframework.beans.factory.annotation.Value
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

    suspend fun getRandomPodcast(): Podcast {
        val podcasts = getBestPodcastsInGenre(140).podcasts


        val randomPodcast = getPodcastById(podcasts.random().id)
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
            previewEpisode = previewEpisode
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

    suspend fun getPodcastById(podcastId: String): ListenNotesPodcast {
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

data class ListenNotesPodcast(
    val id: String,
    val title: String,
    val description: String,
    val thumbnail: String,
    val episodes: List<ListenNotesEpisode> = emptyList()
)

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class ListenNotesEpisode(
    val id: String,
    val title: String,
    val description: String,
    val audio: String,
    val audioLengthSec: Int
)
