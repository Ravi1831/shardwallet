package com.ravi.mds.shardedsagawallet.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class User extends BaseEntity{

    private String username;

    @Column(unique = true)
    private String email;

}
