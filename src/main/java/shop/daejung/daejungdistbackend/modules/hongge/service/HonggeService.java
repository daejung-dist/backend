package shop.daejung.daejungdistbackend.modules.hongge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import shop.daejung.daejungdistbackend.modules.hongge.dto.HonggeResponse;
import shop.daejung.daejungdistbackend.modules.hongge.repository.HonggeRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HonggeService {
    private final HonggeRepository honggeRepository;

    public HonggeResponse getHongge(Long id) {
        return honggeRepository.findById(id)
                .map(HonggeResponse::from)
                .orElseThrow(this::notFound);
    }

    public List<HonggeResponse> getHongges() {
        return honggeRepository.findAllByOrderByIdAsc().stream()
                .map(HonggeResponse::from)
                .toList();
    }

//    재고
    // 주문에서 호출한다. 재고가 모자라면 아무것도 빼지 않고 실패한다
    @Transactional
    public void decreaseStock(Long id, int amount) {
        checkAmount(amount);
        if (honggeRepository.decreaseQuantity(id, amount) == 1) {
            return;
        }
        if (!honggeRepository.existsById(id)) {
            throw notFound();
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "재고가 부족합니다.");
    }

    // 결제 전 주문 취소나 환불 때 빼 둔 재고를 되돌린다
    @Transactional
    public void increaseStock(Long id, int amount) {
        checkAmount(amount);
        if (honggeRepository.increaseQuantity(id, amount) == 0) {
            throw notFound();
        }
    }

    // 음수가 들어오면 차감이 오히려 재고를 늘리게 되므로 막는다
    private void checkAmount(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("재고 변경 수량은 1 이상이어야 합니다.");
        }
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다.");
    }
}
