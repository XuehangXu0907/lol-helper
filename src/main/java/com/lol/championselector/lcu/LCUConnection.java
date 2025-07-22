package com.lol.championselector.lcu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class LCUConnection {
    private static final Logger logger = LoggerFactory.getLogger(LCUConnection.class);
    
    private final String baseUrl;
    private final String authToken;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private boolean isConnected = false;
    
    public LCUConnection(int port, String password) {
        this.baseUrl = "https://127.0.0.1:" + port;
        this.authToken = "Basic " + Base64.getEncoder()
            .encodeToString(("riot:" + password).getBytes());
        this.objectMapper = new ObjectMapper();
        this.httpClient = createHttpClient();
    }
    
    private OkHttpClient createHttpClient() {
        try {
            // 创建信任所有证书的TrustManager
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                    
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
                    }
                }
            };
            
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            return new OkHttpClient.Builder()
                .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                .hostnameVerifier((hostname, session) -> true)
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder()
                        .addHeader("Authorization", authToken)
                        .addHeader("Content-Type", "application/json");
                    return chain.proceed(requestBuilder.build());
                })
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create HTTP client", e);
        }
    }
    
    public CompletableFuture<Boolean> testConnection() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                    .url(baseUrl + "/lol-gameflow/v1/gameflow-phase")
                    .get()
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    isConnected = response.isSuccessful();
                    if (isConnected) {
                        logger.info("Successfully connected to LCU at {}", baseUrl);
                    } else {
                        logger.warn("Failed to connect to LCU: HTTP {}", response.code());
                    }
                    return isConnected;
                }
            } catch (Exception e) {
                isConnected = false;
                logger.error("Connection test failed", e);
                return false;
            }
        });
    }
    
    public CompletableFuture<JsonNode> get(String endpoint) {
        return makeRequest("GET", endpoint, null);
    }
    
    public CompletableFuture<JsonNode> post(String endpoint, Object body) {
        return makeRequest("POST", endpoint, body);
    }
    
    public CompletableFuture<JsonNode> put(String endpoint, Object body) {
        return makeRequest("PUT", endpoint, body);
    }
    
    public CompletableFuture<JsonNode> patch(String endpoint, Object body) {
        return makeRequest("PATCH", endpoint, body);
    }
    
    private CompletableFuture<JsonNode> makeRequest(String method, String endpoint, Object body) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request.Builder requestBuilder = new Request.Builder()
                    .url(baseUrl + endpoint);
                
                RequestBody requestBody = null;
                if (body != null) {
                    String json = objectMapper.writeValueAsString(body);
                    requestBody = RequestBody.create(json, MediaType.get("application/json"));
                }
                
                switch (method.toUpperCase()) {
                    case "GET":
                        requestBuilder.get();
                        break;
                    case "POST":
                        requestBuilder.post(requestBody != null ? requestBody : 
                            RequestBody.create("", MediaType.get("application/json")));
                        break;
                    case "PUT":
                        requestBuilder.put(requestBody != null ? requestBody : 
                            RequestBody.create("", MediaType.get("application/json")));
                        break;
                    case "PATCH":
                        requestBuilder.patch(requestBody != null ? requestBody : 
                            RequestBody.create("", MediaType.get("application/json")));
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported HTTP method: " + method);
                }
                
                Request request = requestBuilder.build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        return objectMapper.readTree(responseBody);
                    } else {
                        logger.warn("Request failed: {} {} - HTTP {}", method, endpoint, response.code());
                        return objectMapper.createObjectNode();
                    }
                }
            } catch (Exception e) {
                logger.error("Request failed: {} {}", method, endpoint, e);
                return objectMapper.createObjectNode();
            }
        });
    }
    
    public boolean isConnected() {
        return isConnected;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void shutdown() {
        try {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
            logger.info("LCU connection shut down successfully");
        } catch (Exception e) {
            logger.warn("Error during LCU connection shutdown", e);
        }
    }
}