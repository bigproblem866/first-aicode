package com.erp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.net.InetAddress;

/**
 * ERP 启动类。
 * <p>
 * 包名刻意放在 <b>com.erp</b> 而不是 com.erp.admin：{@code @SpringBootApplication}
 * 的组件扫描范围是「启动类所在包及其子包」，放在 com.erp 才能扫到
 * com.erp.system / com.erp.mdm / com.erp.inventory / com.erp.purchase 里的
 * @Controller、@Service。
 * <p>
 * 本类不做 {@code @MapperScan}：各 Mapper 接口上直接标注 {@code @Mapper}，
 * 由 MyBatis 的自动配置扫描。这样新增模块时不需要回来改启动类。
 */
@Slf4j
@SpringBootApplication
public class ErpApplication {

    public static void main(String[] args) {
        Environment env = SpringApplication.run(ErpApplication.class, args).getEnvironment();
        printStartupInfo(env);
    }

    private static void printStartupInfo(Environment env) {
        String port = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String host = "localhost";
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignored) {
            // 取不到本机 IP 不影响启动，用 localhost 兜底
        }
        String profile = String.join(",", env.getActiveProfiles());

        log.info("""

                        ----------------------------------------------------------
                        ERP 启动成功
                          环境:     {}
                          本地地址: http://localhost:{}{}
                          接口文档: http://localhost:{}{}/doc.html
                          外部访问: http://{}:{}{}
                        ----------------------------------------------------------""",
                profile.isEmpty() ? "default" : profile,
                port, contextPath,
                port, contextPath,
                host, port, contextPath);
    }
}
