package com.deveagles.be15_deveagles_be.features.users.command.application.service;

import com.deveagles.be15_deveagles_be.features.users.command.application.dto.request.CreateStaffRequest;
import com.deveagles.be15_deveagles_be.features.users.command.application.dto.request.PutStaffRequest;
import com.deveagles.be15_deveagles_be.features.users.command.application.dto.response.StaffInfoResponse;
import com.deveagles.be15_deveagles_be.features.users.command.application.dto.response.StaffProfileUrlResponse;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

public interface StaffCommandService {
  void staffCreate(Long shopId, CreateStaffRequest staffRequest, MultipartFile profile);

  StaffInfoResponse getStaffDetail(Long staffId);

  void putStaffDetail(Long staffId, @Valid PutStaffRequest staffRequest, MultipartFile profile);

  StaffProfileUrlResponse getStaffProfileUrl(Long staffId);
}
