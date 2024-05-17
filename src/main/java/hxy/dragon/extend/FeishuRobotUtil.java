package hxy.dragon.extend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class FeishuRobotUtil {

    private static final Logger log = LoggerFactory.getLogger(FeishuRobotUtil.class);

    private static TenantAccessTokenBody tenantAccessToken;

    public static void feishuRobotDetail(String msg) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String callerInfo = stackTrace[2].getFileName() + ":" + stackTrace[2].getLineNumber();
        String content = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                "【air-share】" + getHostName() + "," + callerInfo + ":" + msg;
        feishuRobot(content);
    }

    private static void feishuRobot(String textContent) {
        if (!isTokenValid(tenantAccessToken)) {
            tenantAccessToken = getTenantAccessToken();
        }

        if (tenantAccessToken != null) {
            String content = textContent;
            try {
                URL url = new URL("https://open.feishu.cn/open-apis/im/v1/messages?receive_id_type=chat_id");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + tenantAccessToken.getTenantAccessToken());
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setDoOutput(true);

                String jsonInputString = "{\"receive_id\": \"oc_0dcaa407df30d1a3415c382e397dcd0f\"," +
                        "\"msg_type\": \"text\"," +
                        "\"content\": \"{\\\"text\\\":\\\"" + content + "\\\"}\"}";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                String responseBody;
                try (InputStream inputStream = conn.getInputStream()) {
                    responseBody = new Scanner(inputStream, StandardCharsets.UTF_8).useDelimiter("\\A").next();
                    System.out.println("飞书请求结果" + responseBody + ", 参数 " + content);
                } catch (IOException exception) {
                    // If an error occurs, read the error stream instead
                    responseBody = readInputStream(conn.getErrorStream());
                    log.error("{}", responseBody);
                }

            } catch (IOException e) {
                log.error("{}", e.getMessage(), e);
            }
        } else {
            log.error("飞书机器人发送消息错误 {}", textContent);
        }
    }

    private static String readInputStream(InputStream inputStream) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    private static boolean isTokenValid(TenantAccessTokenBody tenantAccessToken) {
        // Your validation logic goes here
        // Check if the token is still valid
        if (tenantAccessToken == null) {
            return false;
        }
        if (tenantAccessToken.getRequestTime().toInstant(ZoneOffset.ofHours(8)).toEpochMilli() + tenantAccessToken.getExpire() * 1000 > System.currentTimeMillis()) {
            return true;
        }
        return false;
    }

    private static TenantAccessTokenBody getTenantAccessToken() {
        String appId = "cli_a1cefb050579500b";
        String appSecret = "PAtVnWyuRQTyRRQ1EpHQ9fnAevpYGkkV";

        try {
            URL url = new URL("https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonInputString = "{\"app_id\": \"" + appId + "\", \"app_secret\": \"" + appSecret + "\"}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            try (InputStream inputStream = conn.getInputStream()) {
                String responseBody = new Scanner(inputStream, StandardCharsets.UTF_8).useDelimiter("\\A").next();
                // Parse the response and return the tenant access token
                return parseAccessToken(responseBody);
            }

        } catch (IOException e) {
            log.error("{}", e.getMessage(), e);
        }

        return null;
    }

    static ObjectMapper objectMapper = new ObjectMapper();

    private static TenantAccessTokenBody parseAccessToken(String response) {
        TenantAccessTokenBody tenantAccessTokenBody = new TenantAccessTokenBody();
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            Integer expire = jsonNode.get("expire").asInt();
            String tenantAccessToken = jsonNode.get("tenant_access_token").asText();
            tenantAccessTokenBody.setExpire(expire);
            tenantAccessTokenBody.setTenantAccessToken(tenantAccessToken);
            tenantAccessTokenBody.setRequestTime(LocalDateTime.now());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // Parse the JSON response and extract the access token
        return tenantAccessTokenBody; // Implement your logic here
    }

    private static String getHostName() {
        // Implement your logic to get the host name
        try {
            return InetAddress.getLocalHost().getHostName(); // For example, return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.error("获取主机名字失败 {}", e.getMessage(), e);
            return null;
        }
    }

}
