package com.deveagles.be15_deveagles_be.features.workflows.command.application.service;

import com.deveagles.be15_deveagles_be.features.workflows.command.application.dto.request.CreateWorkflowCommand;
import com.deveagles.be15_deveagles_be.features.workflows.command.application.dto.request.DeleteWorkflowCommand;
import com.deveagles.be15_deveagles_be.features.workflows.command.application.dto.request.ToggleWorkflowCommand;
import com.deveagles.be15_deveagles_be.features.workflows.command.application.dto.request.UpdateWorkflowCommand;

public interface WorkflowCommandService {

  Long createWorkflow(CreateWorkflowCommand command);

  void updateWorkflow(UpdateWorkflowCommand command);

  void deleteWorkflow(DeleteWorkflowCommand command);

  void toggleWorkflowStatus(ToggleWorkflowCommand command);
}
