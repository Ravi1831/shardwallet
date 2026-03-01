package com.ravi.mds.shardedsagawallet.controller;

import com.ravi.mds.shardedsagawallet.dtos.TransferResponseDto;
import com.ravi.mds.shardedsagawallet.dtos.TransferRequestDto;
import com.ravi.mds.shardedsagawallet.service.TransactionService;
import com.ravi.mds.shardedsagawallet.service.TransferSagaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1/transaction")
@RestController
public class TransactionController {

    private final TransferSagaService transferSagaService;

    @PostMapping
    public ResponseEntity<TransferResponseDto> createTransaction(@RequestBody TransferRequestDto requestDto) {
        Long sagaInstanceId = transferSagaService.initiateTransaction(
                requestDto.getFromWalletId(),
                requestDto.getToWalletId(),
                requestDto.getAmount(),
                requestDto.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                TransferResponseDto.builder()
                        .sagaInstanceId(sagaInstanceId)
                        .build());
    }

}
