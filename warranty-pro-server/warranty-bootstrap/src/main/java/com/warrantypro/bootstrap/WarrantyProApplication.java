package com.warrantypro.bootstrap;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * WarrantyPro 数字化物业保修平台 —— 后端启动入口。
 *
 * <p>模块化单体：各业务模块依赖关系见 warranty-pro-server/pom.xml 与 docs/04 §2，
 * 模块间调用走 Spring Bean 接口注入，为微服务拆分预留边界。
 * scanBasePackages 覆盖全部模块（默认只扫 bootstrap 包）；@EnableScheduling 供
 * 到场超时改派与业主验收超时自动通过定时任务使用（docs/03 §5.2、§6）。</p>
 */
@SpringBootApplication(scanBasePackages = "com.warrantypro")
@MapperScan("com.warrantypro.**.mapper")
@EnableScheduling
public class WarrantyProApplication {

    public static void main(String[] args) {
        SpringApplication.run(WarrantyProApplication.class, args);
    }
}
