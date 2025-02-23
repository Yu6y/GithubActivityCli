package src;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class GithubActivity {
    public static void main(String[] args){
        if(args.length != 1)
            System.out.println("Invalid parameter.");
        else
            try {
                handleRequest(args[0]);
            }catch(ActivityException e){
                System.out.println(e.getMessage());
            }

    }

    public static void handleRequest(String username)throws ActivityException{
        HttpClient httpClient = HttpClient.newHttpClient();
        URI uri;

        try{
            uri = new URI(String.format("https://api.github.com/users/%s/events", username));
        }catch(URISyntaxException e){
            throw new ActivityException(e.getMessage());
        }

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();
        JsonNode response = null;
        Map<String, Object>[] cos;
        try{
            HttpResponse<String> httpResponse = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            if(httpResponse.statusCode() == 404)
                throw new ActivityException("API couldn't return data. Check url or username.");
            else if(httpResponse.statusCode() >= 500)
                throw new ActivityException("Server error.");

            response = new ObjectMapper().readTree(httpResponse.body());
            cos = new ObjectMapper().readValue(httpResponse.body(), Map[].class);
        }catch(IOException | InterruptedException e){
            throw new ActivityException(e.getMessage());
        }

        if(response == null)
            throw new ActivityException("Cannot get data from API.");

        for(Map<String, Object> map: cos){
            for(String key: map.keySet()){
                System.out.println(key + " - " + map.get(key));
            }
        }

        for(JsonNode node: response) {
            String created_at;
            try {
                ZonedDateTime date = ZonedDateTime.parse(node.get("created_at").asText());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                created_at = date.format(formatter);
            }catch(Exception e){
                throw new ActivityException(e.getMessage());
            }

            switch(node.get("type").asText()){
                case "CreateEvent":
                    System.out.println("Created " + node.get("payload").get("ref_type").asText() + " in " + node.get("repo").get("name").asText() + " at " +
                            created_at);
                    break;
                case "PushEvent":
                    System.out.println("Pushed " +  node.get("payload").get("size").asText() + " commits to "
                            + node.get("repo").get("name").asText() + " at " + created_at);
                    break;
                case "IssuesEvent":
                    System.out.println("Opened a new issue in " + node.get("repo").get("name").asText() +" at " + created_at);
                    break;
                case "ForkEvent":
                    System.out.println("Forked " + node.get("repo").get("name").asText() +" at " + created_at);
                    break;
                case "WatchEvent":
                    System.out.println("Starred " + node.get("repo").get("name").asText() +" at " + created_at);
                    break;
                default:
                    System.out.println(node.get("type").asText().replace("Event", "") + " in " + node.get("repo").get("name").asText() + " at " + created_at);
            }
        }
    }
}
