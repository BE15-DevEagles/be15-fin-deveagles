package com.deveagles.be15_deveagles_be.features.workflows.command.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ToggleWorkflowCommand {

  @NotNull(message = "워크플로우 ID는 필수입니다") private Long workflowId;

  @NotNull(message = "매장 ID는 필수입니다") private Long shopId;

  @NotNull(message = "직원 ID는 필수입니다") private Long staffId;
}
