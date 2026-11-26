package shop.daejung.daejungdistbackend.global.util;

import java.util.Locale;

public final class EmailNormalizer {
    private EmailNormalizer() {
    }

    // 대소문자만 다른 주소를 같은 이메일로 봐야 인증한 주소와 가입·로그인 주소가 어긋나지 않는다
    public static String normalize(String email) {
        return email == null ? null : email.toLowerCase(Locale.ROOT);
    }
}
