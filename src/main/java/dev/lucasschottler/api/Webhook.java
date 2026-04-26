package dev.lucasschottler.api;

import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Webhook {
    private static final String WEBHOOK_URL = System.getenv("WEBHOOK_URL");
    private static final String FULFILLMENT_WEBHOOK_URL = System.getenv("FULFILLMENT_WEBHOOK_URL");
    private static final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json");
    private static final Logger logger = LoggerFactory.getLogger(Webhook.class);

    public static void sendMessage(String content) {
        try{
            JSONObject payload = new JSONObject();
            payload.put("content", "\n<=>\n" + content + "\n<=>\n");

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
        } catch (Exception e){
            logger.error("Webhook: Unable to send message!: {}", content);
        }
    }

    public static void sendFulfillmentMessage(String content) {
        try{
            JSONObject payload = new JSONObject();
            payload.put("content", "\n<=>\n" + content + "\n<=>\n");

            RequestBody body = RequestBody.create(payload.toString(), JSON);
            Request request = new Request.Builder()
                    .url(FULFILLMENT_WEBHOOK_URL)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Discord error: " + response.code() + " " + response.body().string());
                }
            }
        } catch (Exception e){
            logger.error("Webhook: Unable to send fulfillment message!: {}", content);
        }
    }
}