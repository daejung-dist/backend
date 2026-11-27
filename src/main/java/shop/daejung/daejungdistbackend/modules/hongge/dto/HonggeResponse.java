package shop.daejung.daejungdistbackend.modules.hongge.dto;

import shop.daejung.daejungdistbackend.modules.hongge.domain.Hongge;

// 엔티티를 그대로 내보내면 나중에 추가되는 내부 필드까지 응답에 실리므로 공개할 필드만 고른다
public record HonggeResponse(
        Long id,
        String name,
        Long price,
        Integer quantity,
        String description,
        String thumbnailUrl
) {
    public static HonggeResponse from(Hongge hongge) {
        return new HonggeResponse(
                hongge.getId(),
                hongge.getName(),
                hongge.getPrice(),
                hongge.getQuantity(),
                hongge.getDescription(),
                hongge.getThumbnailUrl()
        );
    }
}
