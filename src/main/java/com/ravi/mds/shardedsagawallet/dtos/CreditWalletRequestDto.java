package com.ravi.mds.shardedsagawallet.dtos;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreditWalletRequestDto {
    private BigDecimal amount;
}
