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

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다.");
    }
}
