package com.ravi.mds.shardedsagawallet.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(WalletNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleWalletNotFoundException(
                        WalletNotFoundException ex,
                        HttpServletRequest request) {
                ErrorResponse error = ErrorResponse.builder()
                                .message(ex.getMessage())
                                .status(HttpStatus.NOT_FOUND.value())
                                .path(request.getPathInfo())
                                .timestamp(LocalDateTime.now())
                                .build();
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(WalletInactiveException.class)
        public ResponseEntity<ErrorResponse> handleWalletInactiveException(
                        WalletInactiveException ex,
                        HttpServletRequest request) {
                ErrorResponse error = ErrorResponse.builder()
                                .message(ex.getMessage())
                                .status(HttpStatus.CONFLICT.value())
                                .path(request.getPathInfo())
                                .timestamp(LocalDateTime.now())
                                .build();
                return new ResponseEntity<>(error, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(InsufficientBalanceException.class)
        public ResponseEntity<ErrorResponse> handleInsufficientBalanceException(
                        InsufficientBalanceException ex,
                        HttpServletRequest request) {
                ErrorResponse error = ErrorResponse.builder()
                                .message(ex.getMessage())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .path(request.getPathInfo())
                                .timestamp(LocalDateTime.now())
                                .build();
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(
                        Exception ex,
                        HttpServletRequest request) {
                ErrorResponse error = ErrorResponse.builder()
                                .message("An unexpected error occurred: " + ex.getMessage())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .path(request.getPathInfo())
                                .timestamp(LocalDateTime.now())
                                .build();
                return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
}
