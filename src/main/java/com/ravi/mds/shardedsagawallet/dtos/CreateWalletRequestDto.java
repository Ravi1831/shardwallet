package com.ravi.mds.shardedsagawallet.dtos;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateWalletRequestDto {
    private long userId;
}
