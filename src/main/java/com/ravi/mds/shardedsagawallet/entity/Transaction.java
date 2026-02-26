package com.ravi.mds.shardedsagawallet.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "transaction")
public class Transaction extends BaseEntity{

    @Column(name = "from_wallet_id",nullable = false)
    private Long fromWalletId;

    @Column(name = "to_wallet_id",nullable = false)
    private Long toWalletId;

    @Column(name = "amount",nullable = false,precision = 19,scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status",nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type",nullable = false)
    @Builder.Default
    private TransactionType transactionType = TransactionType.TRANSFER;

    @Column(name = "description",nullable = true)
    private String description;

    @Column(name = "saga_instance_id",nullable = false)
    private Long sagaInstanceId;
}
