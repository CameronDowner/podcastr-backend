package com.github.camerondowner.podcaster.service

import com.github.camerondowner.podcaster.controller.Podcast
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody



@Service
class SpotifyService(
    webClientBuilder: WebClient.Builder
) {
    private val webClient = webClientBuilder.baseUrl("https://api.spotify.com/v1/")
        .build()

    @Cacheable("spotify-shows")
    open fun getShowsForAccessTokenAsync(accessTokenValue: String): Deferred<SpotifyResponse> {
        println("Getting shows")
        return GlobalScope.async {
            webClient.get()
                .uri("/me/shows")
                .headers { it.setBearerAuth(accessTokenValue) }
                .retrieve()
                .awaitBody()
        }
    }
}

data class SpotifyResponse(
    val items: List<SpotifyItem>
)

data class SpotifyItem(
    val show: SpotifyShow
)

data class SpotifyShow(
    val name: String,
    val description: String
)
