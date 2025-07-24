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
    private volatile boolean isConnected = false;
    private volatile boolean isShuttingDown = false;
    
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
                .connectTimeout(3, TimeUnit.SECONDS) // Reduced timeout
                .readTimeout(8, TimeUnit.SECONDS)   // Reduced timeout
                .writeTimeout(5, TimeUnit.SECONDS)  // Add write timeout
                .connectionPool(new okhttp3.ConnectionPool(5, 5, TimeUnit.MINUTES)) // Limited connection pool
                .addInterceptor(chain -> {
                    if (isShuttingDown) {
                        throw new java.io.IOException("Connection is shutting down");
                    }
                    
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder()
                        .addHeader("Authorization", authToken)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Connection", "close"); // Prevent connection reuse issues
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
        if (isShuttingDown) {
            return CompletableFuture.completedFuture(objectMapper.createObjectNode().put("error", "Connection is shutting down"));
        }
        
        return CompletableFuture.supplyAsync(() -> {
            if (isShuttingDown) {
                return objectMapper.createObjectNode().put("error", "Connection is shutting down");
            }
            
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
                    if (isShuttingDown) {
                        return objectMapper.createObjectNode().put("error", "Connection is shutting down");
                    }
                    
                    String responseBodyString = "";
                    if (response.body() != null) {
                        responseBodyString = response.body().string();
                    }
                    
                    if (response.isSuccessful()) {
                        logger.debug("Request successful: {} {} - HTTP {} - Response: {}", 
                                   method, endpoint, response.code(), 
                                   responseBodyString.length() > 300 ? responseBodyString.substring(0, 300) + "..." : responseBodyString);
                        
                        if (!responseBodyString.isEmpty()) {
                            try {
                                return objectMapper.readTree(responseBodyString);
                            } catch (Exception parseException) {
                                logger.warn("Failed to parse JSON response for {} {}: {}", method, endpoint, parseException.getMessage());
                                return objectMapper.createObjectNode().put("error", "Invalid JSON response");
                            }
                        } else {
                            // 返回成功的空对象（某些操作可能没有响应体）
                            return objectMapper.createObjectNode().put("success", true);
                        }
                    } else {
                        logger.error("Request failed: {} {} - HTTP {} - Response: {}", 
                                   method, endpoint, response.code(), 
                                   responseBodyString.length() > 200 ? responseBodyString.substring(0, 200) + "..." : responseBodyString);
                        return objectMapper.createObjectNode().put("error", true).put("status", response.code());
                    }
                }
            } catch (java.net.SocketTimeoutException e) {
                if (!isShuttingDown) {
                    logger.debug("Request timeout: {} {} - {}", method, endpoint, e.getMessage());
                }
                return objectMapper.createObjectNode().put("error", "timeout");
            } catch (java.io.IOException e) {
                if (!isShuttingDown) {
                    logger.debug("IO error for request: {} {} - {}", method, endpoint, e.getMessage());
                }
                return objectMapper.createObjectNode().put("error", "io_error");
            } catch (Exception e) {
                if (!isShuttingDown) {
                    logger.error("Request failed: {} {} - {}", method, endpoint, e.getMessage());
                }
                return objectMapper.createObjectNode().put("error", "exception");
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
        logger.info("Shutting down LCU connection...");
        isShuttingDown = true;
        isConnected = false;
        
        try {
            // Cancel all pending calls
            httpClient.dispatcher().cancelAll();
            
            // Shutdown dispatcher executor service
            httpClient.dispatcher().executorService().shutdown();
            try {
                if (!httpClient.dispatcher().executorService().awaitTermination(5, TimeUnit.SECONDS)) {
                    httpClient.dispatcher().executorService().shutdownNow();
                }
            } catch (InterruptedException e) {
                httpClient.dispatcher().executorService().shutdownNow();
                Thread.currentThread().interrupt();
            }
            
            // Close all connections
            httpClient.connectionPool().evictAll();
            
            logger.info("LCU connection shut down successfully");
        } catch (Exception e) {
            logger.warn("Error during LCU connection shutdown", e);
        }
    }
}