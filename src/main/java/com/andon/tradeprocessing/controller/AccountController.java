package com.andon.tradeprocessing.controller;

import com.andon.tradeprocessing.dto.AccountResponse;
import com.andon.tradeprocessing.dto.ApiResponse;
import com.andon.tradeprocessing.dto.CreateAccountRequest;
import com.andon.tradeprocessing.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Brokerage account management")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "Open a new brokerage account")
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Account created successfully", accountService.createAccount(request)));
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get account details")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.ok(accountService.getAccount(accountId)));
    }

    @GetMapping
    @Operation(summary = "List all accounts")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAllAccounts() {
        return ResponseEntity.ok(ApiResponse.ok(accountService.getAllAccounts()));
    }
}
