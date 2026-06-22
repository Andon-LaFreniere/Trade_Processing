package com.andon.tradeprocessing.service;

import com.andon.tradeprocessing.dto.AccountResponse;
import com.andon.tradeprocessing.dto.CreateAccountRequest;
import com.andon.tradeprocessing.entity.Account;
import com.andon.tradeprocessing.exception.ResourceNotFoundException;
import com.andon.tradeprocessing.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("Username '" + request.getUsername() + "' is already taken");
        }
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email '" + request.getEmail() + "' is already registered");
        }

        Account account = Account.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .cashBalance(request.getInitialDeposit())
                .build();

        account = accountRepository.save(account);
        log.info("Created account id={} username={} balance={}", account.getId(), account.getUsername(), account.getCashBalance());
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long accountId) {
        return toResponse(findById(accountId));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream().map(this::toResponse).toList();
    }

    /**
     * Package-private helper used by other services to load the managed entity.
     */
    Account findById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: id=" + accountId));
    }

    private AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .username(account.getUsername())
                .email(account.getEmail())
                .cashBalance(account.getCashBalance())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
