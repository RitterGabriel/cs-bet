package com.csbet.backend.wallet;

import com.csbet.backend.exception.ApiExceptionHandler.ApiError;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes = WalletController.class)
@Order(-1)
public class WalletExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, jakarta.validation.ConstraintViolationException.class,
            org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> invalid(Exception error) {
        return ResponseEntity.badRequest().body(new ApiError("Informe um valor positivo de até 10.000,00, com até duas casas decimais, e um identificador válido."));
    }
    @ExceptionHandler(org.springframework.dao.ConcurrencyFailureException.class)
    ResponseEntity<ApiError> busy(Exception error) {
        return ResponseEntity.status(409).body(new ApiError("Carteira ocupada. Tente novamente com o mesmo identificador."));
    }
}
