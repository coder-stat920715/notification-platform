package com.acme.notify.preference;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_contact")
@Getter
@Setter
@NoArgsConstructor
public class UserContact {
    @Id
    private String userId;
    private String email;
    private String phone;
    private String deviceToken;
    private String webhookUrl;
}
