package shop.daejung.daejungdistbackend.modules.hongge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import shop.daejung.daejungdistbackend.modules.hongge.domain.Hongge;
import shop.daejung.daejungdistbackend.modules.hongge.repository.HonggeRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HonggeService {
    private final HonggeRepository honggeRepository;

    public Hongge getHongge(Long id) {
        return honggeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hongge not found"));
    }
}
