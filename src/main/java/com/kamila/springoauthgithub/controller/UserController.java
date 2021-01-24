package com.kamila.springoauthgithub.controller;


import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

@Controller
@RequestMapping("/")
public class UserController {

    private static final String GITHUB_API_URL = "https://api.github.com";

    private WebClient webClient;

    public UserController(WebClient webClient) {
        this.webClient = webClient;
    }

    @GetMapping
    public String index(@RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
                        @AuthenticationPrincipal OAuth2User oauth2User,
                        Model model) {
//realiza a autenticação no nevegador para registrar quem é o usuário do github que tenta se autentitcar
        model.addAttribute("repositories", fetchAllRepositories(authorizedClient));
        model.addAttribute("username", oauth2User.getAttributes().get("login"));

        return "index";
    }

    private Flux<String> fetchAllRepositories(OAuth2AuthorizedClient authorizedClient) {
        return this.webClient
                //lista 30 repositórios (ordem alfabética) desse usuário que foi autenticado
                .get()
                .uri(GITHUB_API_URL, uriBuilder ->
                        uriBuilder
                                .path("/user/repos")
                                .queryParam("per_page", 30)
                                .build()
                )
                .attributes(oauth2AuthorizedClient(authorizedClient))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<JsonNode>>() {
                })
                .flatMapMany(Flux::fromIterable)
                .map(jsonNode -> jsonNode.get("full_name").asText());
    }
}