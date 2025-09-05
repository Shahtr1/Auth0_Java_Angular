package com.orders_cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class Main {
    private static final ObjectMapper M = new ObjectMapper();

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
        String clientId = need("AUTH0_CLI_CLIENT_ID");
        String audience = need("API_AUDIENCE");
        String apiBase = need("API_BASE");

        boolean doWrite = args.length > 0 && args[0].equalsIgnoreCase("write");

        // Request device code (include audience + scopes you want)
        var device = requestDeviceCode(domain, clientId,
                audience,
                "openid profile email offline_access read:orders" + (doWrite ? " write:orders" : ""));

        // Tell the user what to do
        System.out.println("\n== Device Login ==");
        System.out.println("Go to: " + device.verificationUri);
        System.out.println("Enter code: " + device.userCode);
        System.out.println("(Tip: if available, visit this full URL) " + device.verificationUriComplete);

        // Poll for token
        var tokens = pollForTokens(domain, clientId, device);

        // Show a tiny summary
        System.out.println("\n== Token received ==");
        System.out.println("access_token (trunc): " + tokens.accessToken.substring(0, 18) + "...");
        if (tokens.refreshToken != null)
            System.out.println("refresh_token present");

        // Call API
        if (doWrite)
            createOrder(apiBase, tokens.accessToken);
        else
            listOrders(apiBase, tokens.accessToken);

    }

    // ---- models ----
    record DeviceResp(String deviceCode, String userCode, String verificationUri, String verificationUriComplete,
            int interval, int expiresIn) {
    }

    record TokenResp(String accessToken, String refreshToken, int expiresIn, String scope) {
    }

    static DeviceResp requestDeviceCode(String domain, String clientId, String audience, String scope)
            throws Exception {
        String url = "https://" + domain + "/oauth/device/code";
        String body = "client_id=" + enc(clientId) + "&scope=" + enc(scope) + "&audience=" + enc(audience);

        HttpResponse<String> res = HTTP().send(HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString());

        if (res.statusCode() / 100 != 2)
            throw new RuntimeException("device code failed: " + res.statusCode() + " " + res.body());

        JsonNode j = M.readTree(res.body());
        return new DeviceResp(
                j.path("device_code").asText(),
                j.path("user_code").asText(),
                j.path("verification_uri").asText(),
                j.path("verification_uri_complete").asText(),
                j.path("interval").asInt(5), // seconds (default to 5)
                j.path("expires_in").asInt(600) // lifespan
        );
    }

    static TokenResp pollForTokens(String domain, String clientId, DeviceResp d) throws Exception {
        String url = "https://" + domain + "/oauth/token";
        int interval = Math.max(3, d.interval); // respect server hint
        long deadline = System.currentTimeMillis() + (d.expiresIn * 1000L);

        while (System.currentTimeMillis() < deadline) {
            String body = "grant_type=urn:ietf:params:oauth:grant-type:device_code"
                    + "&device_code=" + enc(d.deviceCode)
                    + "&client_id=" + enc(clientId);

            HttpResponse<String> res = HTTP().send(HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(15)).build(),
                    HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() / 100 == 2) {
                JsonNode j = M.readTree(res.body());
                return new TokenResp(
                        j.path("access_token").asText(),
                        j.path("refresh_token").asText(null),
                        j.path("expires_in").asInt(0),
                        j.path("scope").asText(""));
            }

            // Handle polling errors per spec
            JsonNode e = M.readTree(res.body());
            String err = e.path("error").asText();
            if ("authorization_pending".equals(err)) {
                Thread.sleep(interval * 1000L);
            } else if ("slow_down".equals(err)) { // back off
                interval += 5;
                Thread.sleep(interval * 1000L);
            } else if ("expired_token".equals(err)) {
                throw new RuntimeException("Device code expired. Start again.");
            } else if ("access_denied".equals(err)) {
                throw new RuntimeException("User denied the request.");
            } else {
                throw new RuntimeException("Token polling failed: " + res.statusCode() + " " + res.body());
            }
        }
        throw new RuntimeException("Timed out waiting for user authorization.");
    }

    static void listOrders(String apiBase, String at) throws Exception {
        var req = HttpRequest.newBuilder(URI.create(apiBase + "/api/orders"))
                .header("Authorization", "Bearer " + at).header("Accept", "application/json").GET().build();
        var res = HTTP().send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("GET /api/orders → " + res.statusCode() + "\n" + res.body());
    }

    static void createOrder(String apiBase, String at) throws Exception {
        var req = HttpRequest.newBuilder(URI.create(apiBase + "/api/orders"))
                .header("Authorization", "Bearer " + at).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"item\":\"CLI-Device-Coffee\"}", StandardCharsets.UTF_8))
                .build();
        var res = HTTP().send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("POST /api/orders → " + res.statusCode() + "\n" + res.body());
    }

    static HttpClient HTTP() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    static String enc(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
