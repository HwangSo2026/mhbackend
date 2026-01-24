package com.hwangso.mhbackend.common.error;

import com.hwangso.mhbackend.common.error.dto.TestValidationRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "4. error-test-controller", description = "전역 예외처리 테스트")
@RestController
@RequestMapping("/api/test/errors")
public class ErrorTestController {

    // ✅ 400: Validation (violations 확인)
    @PostMapping("/validation")
    public ResponseEntity<String> validation(@Valid @RequestBody TestValidationRequest req) {
        return ResponseEntity.ok("OK");
    }

    // ✅ 401 : ApiException UNAUTHORIZED (관리자 인증 실패시)
    @GetMapping("/401")
    public ResponseEntity<Void> unauthorized() {
        throw new ApiException(ErrorCode.UNAUTHORIZED);
    }

    // ✅ 403: ApiException
    @GetMapping("/403")
    public ResponseEntity<Void> forbidden() {
        throw new ApiException(ErrorCode.HOLD_FORBIDDEN);
    }

    // ✅ 404: ApiException
    @GetMapping("/404")
    public ResponseEntity<Void> notFound() {
        throw new ApiException(ErrorCode.HOLD_NOT_FOUND);
    }

    // ✅ 409: ApiException
    @GetMapping("/409")
    public ResponseEntity<Void> conflict() {
        throw new ApiException(ErrorCode.HOLD_CONFLICT);
    }

    // ✅ 410: ApiException
    @GetMapping("/410")
    public ResponseEntity<Void> gone() {
        throw new ApiException(ErrorCode.HOLD_GONE);
    }

    // ✅ 501: RESERVATION_JSON_PARSE_ERROR
    @GetMapping("/501")
    public ResponseEntity<Void> paresError() { throw new ApiException(ErrorCode.RESERVATION_JSON_PARSE_ERROR); }

    // ✅ 500: Unexpected Exception
    @GetMapping("/500")
    public ResponseEntity<Void> internalError() {
        throw new RuntimeException("boom");
    }

}