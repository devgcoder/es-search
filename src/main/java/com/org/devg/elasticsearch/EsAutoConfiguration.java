package com.org.devg.elasticsearch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author devg
 * @Date 2019/10/13 17:46
 */

@Slf4j
@Configuration
@ConditionalOnWebApplication //web应用才生效
public class EsAutoConfiguration {

  @Bean
  public EsSearch esSearch() {
    EsSearch esSearch = new EsSearch();
    log.info("init esSearch sucessful");
    return esSearch;
  }

}
