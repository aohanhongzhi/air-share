package hxy.dragon.extend;

import java.time.LocalDateTime;

public class TenantAccessTokenBody {
    private int code;
    private int expire;
    private LocalDateTime requestTime;
    private String msg;
    private String tenantAccessToken;


    public TenantAccessTokenBody() {
    }

    // Constructor
    public TenantAccessTokenBody(int code, int expire, LocalDateTime requestTime, String msg, String tenantAccessToken) {
        this.code = code;
        this.expire = expire;
        this.requestTime = requestTime;
        this.msg = msg;
        this.tenantAccessToken = tenantAccessToken;
    }

    // Getters and Setters
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getExpire() {
        return expire;
    }

    public void setExpire(int expire) {
        this.expire = expire;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getTenantAccessToken() {
        return tenantAccessToken;
    }

    public void setTenantAccessToken(String tenantAccessToken) {
        this.tenantAccessToken = tenantAccessToken;
    }

    // toString method
    @Override
        public String toString() {
        return "TenantAccessTokenBody{" +
               "code=" + code +
               ", expire=" + expire +
               ", requestTime=" + requestTime +
               ", msg='" + msg + '\'' +
               ", tenantAccessToken='" + tenantAccessToken + '\'' +
               '}';
    }
}
