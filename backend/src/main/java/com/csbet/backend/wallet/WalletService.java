package com.csbet.backend.wallet;

import com.csbet.backend.entity.User;
import com.csbet.backend.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class WalletService {
    private final UserRepository users;
    private final WalletMovementRepository movements;
    public WalletService(UserRepository users, WalletMovementRepository movements) {
        this.users = users;
        this.movements = movements;
    }
    @Transactional(readOnly = true)
    public WalletResponse.Balance balance(Long userId) {
        return new WalletResponse.Balance(authenticated(userId).getBalance().toPlainString());
    }
    @Transactional
    public WalletResponse.Balance add(Long userId, @Valid AddCreditsRequest request) {
        // O bloqueio permanece até o commit: saldo e histórico são uma única operação.
        User user = users.findByIdForUpdate(userId).orElseThrow(() -> new BadCredentialsException("Não autenticado."));
        var previous = movements.findByUserIdAndRequestId(userId, request.requestId().toString());
        if (previous.isPresent()) {
            if (!"CREDIT_ADDITION".equals(previous.get().getType()) || previous.get().getAmount().compareTo(request.amount()) != 0)
                throw new IllegalArgumentException("Identificador já utilizado com outro valor.");
            return new WalletResponse.Balance(user.getBalance().toPlainString());
        }
        user.addCredits(request.amount());
        movements.save(new WalletMovement(user, request.amount(), request.requestId().toString()));
        return new WalletResponse.Balance(user.getBalance().toPlainString());
    }
    @Transactional(readOnly = true)
    public WalletResponse.History history(Long userId, int page) {
        if (page < 0 || page > 100000) throw new IllegalArgumentException("Página inválida.");
        authenticated(userId);
        var result = movements.findByUserId(userId, PageRequest.of(page, 20,
                Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        return new WalletResponse.History(result.map(WalletResponse.Movement::from).getContent(),
                result.getNumber(), result.getTotalPages(), result.getTotalElements());
    }
    private User authenticated(Long userId) {
        return users.findById(userId).orElseThrow(() -> new BadCredentialsException("Não autenticado."));
    }
}
