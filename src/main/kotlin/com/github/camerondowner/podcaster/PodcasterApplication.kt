package com.github.camerondowner.podcaster

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.DefaultServerRedirectStrategy
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.ServerRedirectStrategy
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import org.springframework.security.web.server.authentication.logout.HttpStatusReturningServerLogoutSuccessHandler
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.net.URI
import java.security.Principal


@SpringBootApplication
@EnableCaching
class PodcasterApplication

fun main(args: Array<String>) {
    runApplication<PodcasterApplication>(*args)
}

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {
    @Bean
    fun securityWebFilterChain(
        http: ServerHttpSecurity
    ): SecurityWebFilterChain {
        return http.authorizeExchange()
            .anyExchange().authenticated()
            .and()
            .oauth2Login {
                it.authenticationSuccessHandler(HardRedirectServerAuthenticationSuccessHandler("/home"))
            }
            .logout {
                it.logoutUrl("/api/logout")
                    .requiresLogout(ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/api/logout"))
                    .logoutSuccessHandler(HttpStatusReturningServerLogoutSuccessHandler(HttpStatus.NO_CONTENT))
            }
            .exceptionHandling {
                it.authenticationEntryPoint(HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
            }
            .build()
    }
}

class HardRedirectServerAuthenticationSuccessHandler(private val location: URI) : ServerAuthenticationSuccessHandler {
    constructor(location: String) : this(URI.create(location))

    private val redirectStrategy: ServerRedirectStrategy = DefaultServerRedirectStrategy()

    override fun onAuthenticationSuccess(
        webFilterExchange: WebFilterExchange,
        authentication: Authentication?
    ): Mono<Void> {
        return redirectStrategy.sendRedirect(webFilterExchange.exchange, location)

    }
}

@RestController
class BaseController {
    @GetMapping("/api/me")
    fun get(): Mono<Any> = Mono.empty()
}
