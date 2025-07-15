import java.io.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class TestCommunityDragon {
    public static void main(String[] args) {
        System.out.println("Testing Community Dragon API...");
        
        try {
            HttpClient client = HttpClient.newHttpClient();
            
            // 测试基础URL
            String baseUrl = "https://raw.communitydragon.org/latest/game/data/characters/";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Base URL Status: " + response.statusCode());
            
            if (response.statusCode() == 200) {
                System.out.println("Response preview: " + response.body().substring(0, Math.min(200, response.body().length())) + "...");
            }
            
            // 测试特定英雄数据
            String aatroxUrl = baseUrl + "aatrox/aatrox.bin.json";
            HttpRequest aatroxRequest = HttpRequest.newBuilder()
                .uri(URI.create(aatroxUrl))
                .build();
            
            HttpResponse<String> aatroxResponse = client.send(aatroxRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("Aatrox data Status: " + aatroxResponse.statusCode());
            
            if (aatroxResponse.statusCode() == 200) {
                System.out.println("Aatrox data preview: " + aatroxResponse.body().substring(0, Math.min(500, aatroxResponse.body().length())) + "...");
            }
            
        } catch (Exception e) {
            System.out.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}