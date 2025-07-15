import java.io.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class TestSync {
    public static void main(String[] args) {
        System.out.println("Starting data sync test...");
        
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ddragon.leagueoflegends.com/api/versions.json"))
                .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                System.out.println("API connection successful!");
                System.out.println("Version data: " + response.body().substring(0, Math.min(100, response.body().length())) + "...");
                
                File dataDir = new File("champion_data");
                if (!dataDir.exists()) {
                    dataDir.mkdirs();
                    System.out.println("Created data directory: " + dataDir.getAbsolutePath());
                }
                
                System.out.println("Data sync test successful! Now running full sync...");
            } else {
                System.out.println("API connection failed, status: " + response.statusCode());
            }
            
        } catch (Exception e) {
            System.out.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}