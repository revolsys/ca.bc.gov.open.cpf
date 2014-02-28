package ca.bc.gov.open.cpf.api.web.controller;

public abstract class AbstractJobController implements JobController {
  @Override
  public void createJobInputFile(final long jobId, final String contentType,
    final Object data) {
    createJobFile(jobId, JOB_INPUTS, 1, contentType, data);
  }
}
