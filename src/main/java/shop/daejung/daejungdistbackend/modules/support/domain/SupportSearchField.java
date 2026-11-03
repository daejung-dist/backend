package shop.daejung.daejungdistbackend.modules.support.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

public enum SupportSearchField {
    ALL,
    TITLE,
    AUTHOR;

    // 프론트는 all·title·author처럼 소문자로 보내므로 대소문자를 가리지 않는다
    public static SupportSearchField from(String value) {
        return Arrays.stream(values())
                .filter(field -> field.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "검색 범위가 올바르지 않습니다."));
    }
}
