package com.orders_worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class Main {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static String need(String k) {
        String v = System.getenv(k);
        if (v == null || v.isBlank()) {
            System.err.println("Missing env: " + k);
            System.exit(2);
        }
        return v;
    }

    public static void main(String[] args) throws Exception {
        String domain = need("AUTH0_DOMAIN");
        String clientId = need("AUTH0_CLIENT_ID");
        String clientSecret = need("AUTH0_CLIENT_SECRET");
        String audience = need("API_AUDIENCE");
        String apiBase = need("API_BASE");

        // mode: "read" (default) or "write"
        String mode = (args.length > 0 ? args[0] : "read").toLowerCase();

        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        // 1) Get access token via Client Credentials
        String token = fetchAccessToken(http, domain, clientId, clientSecret, audience);
        System.out.println("Got access token (truncated): " + token.substring(0, Math.min(20, token.length())) + "...");

        // 2) Call the API
        if (mode.equals("write")) {
            callCreateOrder(http, apiBase, token);
        } else {
            callListOrders(http, apiBase, token);
        }
    }

    private static String fetchAccessToken(HttpClient http, String domain, String clientId,
            String clientSecret, String audience) throws Exception {
        String url = "https://" + domain + "/oauth/token";
        String body = """
                {
                  "grant_type": "client_credentials",
                  "client_id": "%s",
                  "client_secret": "%s",
                  "audience": "%s"
                }
                """.formatted(escape(clientId), escape(clientSecret), escape(audience));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) {
            System.err.println("Token request failed: HTTP " + res.statusCode() + " → " + res.body());
            System.exit(3);
        }
        JsonNode json = MAPPER.readTree(res.body());
        String accessToken = json.path("access_token").asText(null);
        String scope = json.path("scope").asText("");
        int expiresIn = json.path("expires_in").asInt(-1);

        if (accessToken == null) {
            System.err.println("No access_token in response: " + res.body());
            System.exit(3);
        }
        System.out.println("Token scope: " + scope + "  (expires_in=" + expiresIn + "s)");
        return accessToken;
    }

    private static void callListOrders(HttpClient http, String apiBase, String token) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + "/api/orders"))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        switch (res.statusCode()) {
            case 200 -> {
                System.out.println("OK (read): " + res.body());
            }
            case 401 -> {
                System.err.println("401 Unauthorized: bad/missing token or wrong audience/issuer.");
            }
            case 403 -> {
                System.err.println(
                        "403 Forbidden: this client lacks 'read:orders'. Grant the scope to the M2M app in Auth0.");
            }
            default -> {
                System.err.println("HTTP " + res.statusCode() + " → " + res.body());
            }
        }
    }

    private static void callCreateOrder(HttpClient http, String apiBase, String token) throws Exception {
        String json = """
                {
                  "item": "Robot-Coffee"
                }
                """;
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + "/api/orders"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        switch (res.statusCode()) {
            case 201 -> {
                System.out.println("Created (write): " + res.body());
            }
            case 401 -> {
                System.err.println("401 Unauthorized: bad/missing token or wrong audience/issuer.");
            }
            case 403 -> {
                System.err.println(
                        "403 Forbidden: this client lacks 'write:orders'. Grant the scope to the M2M app in Auth0.");
            }
            default -> {
                System.err.println("HTTP " + res.statusCode() + " → " + res.body());
            }
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
