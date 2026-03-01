package com.ravi.mds.shardedsagawallet.dtos;

import lombok.*;

import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransferRequestDto {
    private Long fromWalletId;
    private Long toWalletId;
    private BigDecimal amount;
    private String description;
}
