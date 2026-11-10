
package com.fanlink.domain.chat;

public record ChatSocketRequest(
        Long roomId,
        String content
) {
}
