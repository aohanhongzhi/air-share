package hxy.dragon.util;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @auther eric
 * @date 2019/3/18 16:44
 */
public class DateUtil {
    /**
     * java8才有的线程安全
     * 止订单不唯一，订单统一由服务器生成。
     * 客户端不生成订单号
     *
     * @return
     */
    public static String getCurrentDateStr() {

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddhhmmssSSS");
        String nowStr = now.format(dtf);

        return nowStr;
    }

    /**
     * 订单号使用bigINT应该有利于数据库检索，毕竟可以使用二分查找等检索方法。提高全局检索能力
     *
     * @return 格式化后的long类型时间
     */
    public static long getCurrentDateLong() {
        //线程不安全
        SimpleDateFormat adf = new SimpleDateFormat("yyyyMMddhhmmssSSS");
        return Long.parseLong(adf.format(new Date()));
    }


    /**
     * 获取系统当前时间戳
     *
     * @param
     * @return currentTime 系统当前时间
     */

    public static Timestamp getCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        Date date = calendar.getTime();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTimeStr = simpleDateFormat.format(date);

        return Timestamp.valueOf(currentTimeStr);
    }

    public static String getTime(long milliseconds) {
        final long day = TimeUnit.MILLISECONDS.toDays(milliseconds);

        final long hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
            - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(milliseconds));

        final long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
            - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds));

        final long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)
            - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds));

        final long ms = TimeUnit.MILLISECONDS.toMillis(milliseconds)
            - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(milliseconds));
        StringBuffer stringBuilder = new StringBuffer();
        if (day > 0) {
            stringBuilder.append(day + "天 ");
        }
        if (hours > 0) {
            stringBuilder.append(hours + "小时 ");
        }
        if (minutes > 0) {
            stringBuilder.append(minutes + "分 ");
        }

        if (seconds > 0) {
            stringBuilder.append(seconds + "秒 ");
        }

//        stringBuilder.append(ms);

        return stringBuilder.toString();
    }

    public static String getNowDate() {
        LocalDate localDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String date = localDate.format(formatter);
        return date;
    }


    public static void main(String[] args) {
        System.out.println(getCurrentDateStr());
        System.out.println(getCurrentDateLong());
    }
}
