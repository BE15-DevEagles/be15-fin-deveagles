package com.deveagles.be15_deveagles_be.features.chat.query.service;

import com.deveagles.be15_deveagles_be.features.chat.query.dto.response.ChatRoomSummaryResponse;
import java.util.List;

public interface ChatRoomQueryService {
  List<ChatRoomSummaryResponse> getMyChatRooms(String userName, Long userId, boolean isStaff);
}
