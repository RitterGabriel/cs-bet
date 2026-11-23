package com.csbet.backend.opening;

import com.csbet.backend.repository.UserRepository;
import com.csbet.backend.wallet.WalletMovementRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpeningBatchService {
    private final UserRepository users;
    private final OpeningBatchRepository batches;
    private final CaseOpeningService opening;
    private final WalletMovementRepository movements;
    private final Clock clock;
    public OpeningBatchService(UserRepository users, OpeningBatchRepository batches, CaseOpeningService opening,
                               WalletMovementRepository movements, Clock pricingClock) {
        this.users = users; this.batches = batches; this.opening = opening; this.movements = movements; this.clock = pricingClock;
    }
    @Transactional
    public OpeningBatchResponse open(Long userId, OpeningBatchRequest request) {
        if (!request.isSupportedQuantity()) throw new OpeningUnavailableException("Escolha 1, 2, 5 ou 10 caixas.");
        // Todas as unidades participam desta transação. Uma falha reverte o lote inteiro.
        var user = users.findByIdForUpdate(userId).orElseThrow(() -> new BadCredentialsException("Não autenticado."));
        var saved = batches.findByUserIdAndRequestId(userId, request.requestId().toString());
        var total = request.expectedPrice().multiply(BigDecimal.valueOf(request.quantity())).setScale(2);
        OpeningBatch batch;
        if (saved.isPresent()) {
            batch = saved.get();
            if (!batch.matches(request)) throw new OpeningUnavailableException("Identificador já utilizado para outro lote.");
        } else {
            if (movements.findByUserIdAndRequestId(userId, request.requestId().toString()).isPresent())
                throw new OpeningUnavailableException("Identificador já utilizado em outra operação.");
            if (user.getBalance().compareTo(total) < 0) throw new OpeningUnavailableException("Saldo fictício insuficiente para todas as caixas.");
            batch = batches.saveAndFlush(new OpeningBatch(user, request, clock.instant()));
        }
        var results = new ArrayList<OpeningResponse>();
        for (int index = 0; index < request.quantity(); index++) {
            UUID id = UUID.nameUUIDFromBytes(("csbet-batch:" + userId + ":" + request.requestId() + ":" + index).getBytes(StandardCharsets.UTF_8));
            results.add(opening.open(userId, new OpeningRequest(request.caseId(), id, request.expectedPrice())));
        }
        return new OpeningBatchResponse(batch.getId(), request.caseId(), request.quantity(), total, results.getLast().balance(), List.copyOf(results));
    }
}
