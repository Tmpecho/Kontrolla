package org.kontrolla.iam.application;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties for outgoing invite emails. */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.mail.invites")
public class MailInviteProperties {

  private boolean enabled;
  private String fromAddress;
  private String fromName = "Kontrolla";
}
