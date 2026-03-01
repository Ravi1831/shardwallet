package com.ravi.mds.shardedsagawallet.controller;

import com.ravi.mds.shardedsagawallet.dtos.CreateWalletRequestDto;
import com.ravi.mds.shardedsagawallet.dtos.CreditWalletRequestDto;
import com.ravi.mds.shardedsagawallet.dtos.DebitWalletRequestDto;
import com.ravi.mds.shardedsagawallet.entity.Wallet;
import com.ravi.mds.shardedsagawallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@Slf4j
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<Wallet> createWallet(@RequestBody CreateWalletRequestDto requestDto) {
        Wallet wallet = walletService.ceateWallet(requestDto.getUserId());
        return new ResponseEntity<>(wallet, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Wallet> getWalletById(@PathVariable Long id) {
        Wallet wallet = walletService.getWalletById(id);
        return new ResponseEntity<>(wallet, HttpStatus.OK);
    }

    @GetMapping("/{userId}/balance")
    public ResponseEntity<BigDecimal> getWalletBalance(@PathVariable Long userId) {
        BigDecimal walletBalance = walletService.getWalletBalance(userId);
        return new ResponseEntity<>(walletBalance, HttpStatus.OK);
    }

    @PostMapping("/{userId}/debit")
    public ResponseEntity<Wallet> debitWallet(@PathVariable Long userId, @RequestBody DebitWalletRequestDto request) {
        walletService.debit(userId, request.getAmount());
        Wallet wallet = walletService.getWalletByUserId(userId);
        return new ResponseEntity<>(wallet, HttpStatus.OK);
    }

    @PostMapping("/{userId}/credit")
    public ResponseEntity<Wallet> creditWallet(@PathVariable Long userId, @RequestBody CreditWalletRequestDto request) {
        walletService.credit(userId, request.getAmount());
        Wallet wallet = walletService.getWalletByUserId(userId);
        return new ResponseEntity<>(wallet, HttpStatus.OK);
    }
}
