package hxy.dragon.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hxy.dragon.entity.reponse.BaseResponse;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

/**
 * @author houxiaoyi
 */
@Slf4j
public class ResponseJsonUtil {

    private static ObjectMapper mapper = new ObjectMapper();

    public static void response(HttpServletResponse httpServletResponse, int status, Integer code, String message, Object param) {
        httpServletResponse.setContentType("application/json;charset=utf-8");
        httpServletResponse.setStatus(status);

        PrintWriter out = null;
        try {
            out = httpServletResponse.getWriter();
            BaseResponse<Object> baseResponse = new BaseResponse<>();

            baseResponse.setCode(code);
            baseResponse.setMsg(message);
            baseResponse.setTimestamp(System.currentTimeMillis());
            baseResponse.setData(param);

            String responesStr = mapper.writeValueAsString(baseResponse);

            out.write(responesStr);
            out.flush();

        } catch (JsonProcessingException e) {
            log.error("\n====>jackson序列化异常", e);
        } catch (IOException e) {
            log.error("\n====>", e);
        } finally {
            if (out != null) {
                out.close();
            }
        }

    }

    public static void responseJson(HttpServletResponse response, Integer code, String message, Object responseObject) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        HashMap<String, Object> baseReponseMap = new HashMap<>();
        baseReponseMap.put("timestamp", System.currentTimeMillis());
        baseReponseMap.put("code", code);
        baseReponseMap.put("message", message);
        String responseData = null;
        try {
            baseReponseMap.put("data", responseObject);
            responseData = mapper.writeValueAsString(baseReponseMap);
        } catch (JsonProcessingException e) {
            log.error("\n====>jackson序列化异常", e);
        }

        PrintWriter out = null;
        try {
            out = response.getWriter();
            out.append(responseData);
        } catch (IOException e) {
            log.error("\n====>", e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
