package dev.lucasschottler.api;

import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONObject;

public class Webhook {
    private static final String WEBHOOK_URL = System.getenv("WEBHOOK_URL");
    private static final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json");

    public static void sendMessage(String content) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("content", content);

        RequestBody body = RequestBody.create(payload.toString(), JSON);
        Request request = new Request.Builder()
                .url(WEBHOOK_URL)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Discord error: " + response.code() + " " + response.body().string());
            }
        }
    }
}