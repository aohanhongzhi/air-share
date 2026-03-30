package hxy.dragon.config.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * 自定义邮箱发送过滤器：
 * - 拒绝包含 URLDecoder/Illegal hex/RequestRejectedException 的 ERROR 日志
 * - 拒绝来自 Tomcat/JUL 框架（org.apache.catalina/org.apache.juli.logging）的容器错误
 * - 拒绝包含断言失败的业务异常日志
 */
public class EmailDenyFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (event == null) {
            return FilterReply.NEUTRAL;
        }

        String loggerName = event.getLoggerName();
        String message = event.getFormattedMessage();

        // 基于 logger 来源的拒绝：Tomcat/JUL 容器错误
        if (loggerName != null) {
            if (loggerName.startsWith("org.apache.catalina") ||
                    loggerName.startsWith("org.apache.juli.logging")) {
                return FilterReply.DENY;
            }
        }

        // 基于消息内容的拒绝：URLDecoder / 防火墙拒绝等
        if (message != null) {
            if (message.contains("URLDecoder") ||
                    message.contains("Illegal hex characters in escape") ||
                    message.contains("RequestRejectedException") ||
                    message.contains("Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception") ||
                    message.contains("uriSessionMapFullCount") ||
                    message.contains("[Assertion failed] - this expression must be true")) {
                return FilterReply.DENY;
            }
        }

        // 检查异常信息
        if (event.getThrowableProxy() != null) {
            String throwableMessage = event.getThrowableProxy().getMessage();
            if (throwableMessage != null && throwableMessage.contains("[Assertion failed] - this expression must be true")) {
                return FilterReply.DENY;
            }
        }

        return FilterReply.NEUTRAL;
    }
}


