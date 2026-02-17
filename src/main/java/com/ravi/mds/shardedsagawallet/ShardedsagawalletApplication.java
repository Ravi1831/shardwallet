package com.ravi.mds.shardedsagawallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ShardedsagawalletApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShardedsagawalletApplication.class, args);
    }

}
