package org.kontrolla.common;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers time-related beans shared across the application. */
@Configuration
public class TimeConfiguration {

  /**
   * Provides the application clock in UTC to keep time handling consistent across services and
   * tests.
   *
   * @return the shared UTC clock
   */
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
