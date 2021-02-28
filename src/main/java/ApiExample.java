
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.VideoListResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collection;


public class ApiExample {
    private static final String CLIENT_SECRETS = "client_secret.json";
    private static final Collection<String> SCOPES =
            Arrays.asList("https://www.googleapis.com/auth/youtube.readonly");

    private static final String APPLICATION_NAME = "API code samples";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    public static Credential authorize(final NetHttpTransport httpTransport) throws IOException {
        InputStream in = ApiExample.class.getResourceAsStream(CLIENT_SECRETS);
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                        .build();

        LocalServerReceiver localReceiver = new LocalServerReceiver.Builder().setPort(8080).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, localReceiver).authorize("user");

        return credential;
    }


    public static YouTube getService() throws GeneralSecurityException, IOException {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = authorize(httpTransport);
        return new YouTube.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }


    public static void main(String[] args)
            throws GeneralSecurityException, IOException, GoogleJsonResponseException {

        YouTube youtubeService = getService();
        YouTube.Videos.List request = youtubeService.videos()
                .list("statistics");
        VideoListResponse response3 = request.setId(getResponse2()).execute();
        System.out.println(response3);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(response3.toString());

        CsvSchema.Builder csvSchemaBuilder = CsvSchema.builder();
        JsonNode firstObject = node.elements().next();
        firstObject.fieldNames().forEachRemaining(csvSchemaBuilder::addColumn);
        CsvSchema csvSchema = csvSchemaBuilder.build().withHeader();


        final CsvMapper csvMapper = new CsvMapper();
        final CsvSchema schema = csvMapper.schemaFor(JsonNode.class);
        final String csv = csvMapper.writer(schema.withUseHeader(true)).writeValueAsString(response3);
        System.out.println(csv);
    }

    public static String getResponse1() throws GeneralSecurityException, IOException {
        YouTube youtubeService = getService();
        YouTube.Channels.List request = youtubeService.channels()
                .list("contentDetails");
        ChannelListResponse response = request.setId("UC_x5XG1OV2P6uZZ5FSM9Ttw").execute();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(response.toString());
        JsonNode items = node.get("items");
        JsonNode contentDetails = items.findValue("contentDetails");
        JsonNode relatedPlaylists = contentDetails.get("relatedPlaylists");
        String uploads = relatedPlaylists.get("uploads").asText();
        return uploads;
    }

    public static String getResponse2() throws GeneralSecurityException, IOException {
        YouTube youtubeService = getService();
        YouTube.Channels.List request = youtubeService.channels()
                .list("contentDetails");

        YouTube.PlaylistItems.List request2 = youtubeService.playlistItems()
                .list("contentDetails");
        PlaylistItemListResponse response2 = request2.setPlaylistId(getResponse1()).execute();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(response2.toString());
        JsonNode items = node.get("items");
        JsonNode contentDetails = items.findValue("contentDetails");
        String videoId = contentDetails.get("videoId").asText();
     //   System.out.println(videoId);
        return videoId;

    }


}
