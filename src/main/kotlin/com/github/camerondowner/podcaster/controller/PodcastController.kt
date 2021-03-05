package com.github.camerondowner.podcaster.controller

import com.github.camerondowner.podcaster.service.ListenNotesService
import com.github.camerondowner.podcaster.service.SpotifyService
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange

@Service
class RecommendationService(
    private val listenNotesService: ListenNotesService,
    private val spotifyService: SpotifyService
) {
    @Cacheable("userRecommendations")
    fun getPodcastRecommendationsAsync(userSpotifyAccessToken: OAuth2AccessToken): Deferred<List<Podcast>> {
        return GlobalScope.async {
            spotifyService.getShowsForAccessTokenAsync(userSpotifyAccessToken.tokenValue).await()
                .items
                .map { listenNotesService.findPodcastByNameAsync(it.show.name) }
                .mapNotNull { it.await() }
                .flatMap { listenNotesService.findPodcastRecommendationsAsync(it).await() }
        }
    }
}

@RestController
class PodcastController(
    private val listenNotesService: ListenNotesService,
    private val spotifyService: RecommendationService,
    private val oauthClientRepo: ServerOAuth2AuthorizedClientRepository
) {
    @GetMapping("/api/recommend_podcast")
    suspend fun recommendPodcast(principal: Authentication, exchange: ServerWebExchange): Podcast {
        val userSpotifyAccessToken = getUserSpotifyAccessToken(principal, exchange)
        val podcastRecommendations = spotifyService.getPodcastRecommendationsAsync(userSpotifyAccessToken)
        val recommendedPodcast = podcastRecommendations.await().random()
        return listenNotesService.getPodcastWithPreviewEpisode(recommendedPodcast.listenNotesId ?: throw IllegalStateException("Don't know id of podcast"))
    }


    private suspend fun getUserSpotifyAccessToken(
        principal: Authentication,
        exchange: ServerWebExchange
    ): OAuth2AccessToken {
        return oauthClientRepo.loadAuthorizedClient<OAuth2AuthorizedClient>("spotify", principal, exchange)
            .awaitSingle()
            .accessToken
    }
}

data class Podcast(
    val title: String,
    val description: String,
    val thumbnail: String? = null,
    val previewEpisode: Episode? = null,
    val listenNotesId: String? = null
)

data class Episode(
    val title: String,
    val description: String,
    val audio: String,
    val audioLengthSec: Int,
    val previewStartingTimeSec: Int
)