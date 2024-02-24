package hxy.dragon.extend;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import hxy.dragon.util.EnvironmentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author eric
 * @program base-server
 * @description 错误消息发送到钉钉上
 * @date 2021/10/28
 */

public class FeishuTalkAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private static final Logger log = LoggerFactory.getLogger(FeishuTalkAppender.class);

    @Retryable(value = Exception.class, maxAttempts = 4, backoff = @Backoff(delay = 100, maxDelay = 500))
    @Override
    protected void append(ILoggingEvent eventObject) {
        String activeProfile = EnvironmentUtil.getActiveProfile();
        Level level = eventObject.getLevel();

        if (activeProfile == null || activeProfile.contains("dev")) {
            if (level.toInt() == Level.ERROR_INT) {
                log.debug("\n====>当前是本地测试环境，错误信息不通知feishu");
            }
            return;
        }
        switch (level.toInt()) {
            case Level.ERROR_INT:
                if (false) {
                    // 发送到feishu
                    ConcurrentHashMap<String, Object> postBody = new ConcurrentHashMap<>();
                    long timeStamp = eventObject.getTimeStamp();

                    // 专属业务日志名字，不同业务响应等级划分
                    String loggerName = eventObject.getLoggerName();
                    String msg = String.format("时间：%s,级别:%s,原因%s", timeStamp, eventObject.getLevel(), eventObject.getFormattedMessage());
                    postBody.put("title", loggerName);
                    postBody.put("msg", msg);
                    FeishuRobotUtil.feishuRobot(msg);
                }
                break;
            case Level.WARN_INT:
            case Level.INFO_INT:
//                log.info("发送日志信息到feishu" + eventObject);
                break;
            default:
                // 默认处理
        }

    }
}
