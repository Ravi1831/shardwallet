package com.ravi.mds.shardedsagawallet.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
public class User extends BaseEntity{

    private String username;

    @Column(unique = true)
    private String email;

}
