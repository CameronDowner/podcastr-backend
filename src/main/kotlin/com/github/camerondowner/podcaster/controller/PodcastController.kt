package com.github.camerondowner.podcaster.controller

import com.github.camerondowner.podcaster.service.ListenNotesService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class PodcastController(private val listenNotesService: ListenNotesService) {
    @GetMapping("/api/recommend_podcast")
    suspend fun recommendPodcast(): Podcast {
        return listenNotesService.getRandomPodcast()
    }
}

data class Podcast(
    val title: String,
    val description: String,
    val thumbnail: String,
    val previewEpisode: Episode?
)

data class Episode(
    val title: String,
    val description: String,
    val audio: String,
    val audioLengthSec: Int,
    val previewStartingTimeSec: Int
)