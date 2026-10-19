package com.erp.framework.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * JSON 序列化的全局约定。
 * <p>
 * 解决三件前端一定会遇到的问题：
 * <ol>
 *   <li><b>Long 精度丢失</b>：JS 的 Number 只精确到 2^53，雪花 ID（19 位）传到前端会变成
 *       末尾几位被抹平的错值，而且报错时完全看不出来。现在用自增主键还碰不到，
 *       但等到换分布式 ID 再改就要翻遍前端。所以第一天就把 Long 序列化成字符串。</li>
 *   <li><b>时间格式</b>：默认会输出成 ISO 数组或 UTC 时间，前端还得转一遍。这里统一成
 *       {@code yyyy-MM-dd HH:mm:ss} 且时区固定 Asia/Shanghai。</li>
 *   <li><b>日期反序列化</b>：前端回传 {@code "2026-10-16"} / {@code "2026-10-16 10:00:00"} 能直接绑上。</li>
 * </ol>
 * 注意：只把<b>包装类型 {@code Long.class}</b> 转字符串，<b>不动基本类型 {@code long}</b>——
 * 这样分页对象的 {@code total}（声明为 long）仍然是数字，el-pagination 拿到的是数字而不是 "42"。
 */
@Configuration
public class JacksonConfig {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String TIME_ZONE = "Asia/Shanghai";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer erpJacksonCustomizer() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);

        return builder -> {
            // Long / 长整型 → 字符串（只针对包装类型，保护 total 等基本类型字段仍为数字）
            builder.serializerByType(Long.class, ToStringSerializer.instance);

            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
            builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
            builder.serializerByType(LocalDate.class, new LocalDateSerializer(dateFormatter));
            builder.deserializerByType(LocalDate.class, new LocalDateDeserializer(dateFormatter));

            builder.simpleDateFormat(DATE_TIME_PATTERN);
            builder.timeZone(TimeZone.getTimeZone(TIME_ZONE));

            // 否则 LocalDateTime 会被写成 [2026,10,16,10,0,0] 这种数组
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        };
    }
}
