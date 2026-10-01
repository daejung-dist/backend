package shop.daejung.daejungdistbackend.modules.hongge.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.daejung.daejungdistbackend.modules.hongge.domain.Hongge;
import shop.daejung.daejungdistbackend.modules.hongge.service.HonggeService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hongges")
@RequiredArgsConstructor
public class HonggeController {
    private final HonggeService honggeService;

    @GetMapping
    public List<Hongge> getHongges() {
        return honggeService.getHongges();
    }

    @GetMapping("/{id}")
    public Hongge getHongge(@PathVariable Long id) {
        return honggeService.getHongge(id);
    }
}
