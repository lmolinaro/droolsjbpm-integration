package org.kie.remote.services.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.drools.core.util.StringUtils;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.model.ProcessDefinition;
import org.jbpm.services.task.audit.commands.DeleteBAMTaskSummariesCommand;
import org.jbpm.services.task.commands.ActivateTaskCommand;
import org.jbpm.services.task.commands.ClaimNextAvailableTaskCommand;
import org.jbpm.services.task.commands.ClaimTaskCommand;
import org.jbpm.services.task.commands.CompleteTaskCommand;
import org.jbpm.services.task.commands.DelegateTaskCommand;
import org.jbpm.services.task.commands.ExitTaskCommand;
import org.jbpm.services.task.commands.FailTaskCommand;
import org.jbpm.services.task.commands.ForwardTaskCommand;
import org.jbpm.services.task.commands.GetContentCommand;
import org.jbpm.services.task.commands.GetTaskCommand;
import org.jbpm.services.task.commands.NominateTaskCommand;
import org.jbpm.services.task.commands.ReleaseTaskCommand;
import org.jbpm.services.task.commands.ResumeTaskCommand;
import org.jbpm.services.task.commands.SkipTaskCommand;
import org.jbpm.services.task.commands.StartTaskCommand;
import org.jbpm.services.task.commands.StopTaskCommand;
import org.jbpm.services.task.commands.SuspendTaskCommand;
import org.jbpm.services.task.commands.TaskCommand;
import org.jbpm.services.task.impl.model.xml.JaxbContent;
import org.jbpm.services.task.impl.model.xml.JaxbTask;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskData;
import org.kie.internal.identity.IdentityProvider;
import org.kie.remote.services.rest.exception.KieRemoteRestOperationException;
import org.kie.remote.services.util.FormURLGenerator;
import org.kie.services.client.serialization.jaxb.impl.process.JaxbProcessDefinition;
import org.kie.services.client.serialization.jaxb.impl.task.JaxbTaskFormResponse;
import org.kie.services.client.serialization.jaxb.rest.JaxbGenericResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This resource provides operations to manage (human) tasks.
 */
@Path("/task")
@RequestScoped
public class TaskResourceImpl extends ResourceBase {

    private static final Logger logger = LoggerFactory.getLogger(RuntimeResourceImpl.class);

    /* REST information */
    @Context
    protected HttpHeaders headers;

    /* KIE information and processing */

    @Inject
    private FormURLGenerator formURLGenerator;

    @Inject
    protected IdentityProvider identityProvider;

    @Inject
    protected QueryResourceImpl queryResource;

    @Inject
    private RuntimeDataService runtimeDataService;

    //    @Inject
    //    private DefinitionService bpmn2DataService;

    private static final String[] allowedOperations = { 
            "activate", 
            "claim", 
            "claimnextavailable", 
            "complete", 
            "delegate", 
            "exit",
            "fail", 
            "forward", 
            "release", 
            "resume", 
            "skip", 
            "start", 
            "stop", 
            "suspend", 
            "nominate", 
    "content"};

    // Rest methods --------------------------------------------------------------------------------------------------------------

    @GET
    @Path("/query")
    @Deprecated
    public Response query() {
        return queryResource.taskSummaryQuery();
    }

    @GET
    @Path("/{taskId: [0-9-]+}")
    public Response getTask(@PathParam("taskId") long taskId) { 
        TaskCommand<?> cmd = new GetTaskCommand(taskId);
        JaxbTask task = (JaxbTask) doRestTaskOperationWithTaskId(taskId, cmd);
        if( task == null ) { 
            throw KieRemoteRestOperationException.notFound("Task " + taskId + " could not be found.");
        }
        return createCorrectVariant(task, headers);
    }

    @POST
    @Path("/{taskId: [0-9-]+}/{oper: [a-zA-Z]+}")
    public Response doTaskOperation(@PathParam("taskId") long taskId, @PathParam("oper") String operation) { 
        Map<String, String[]> params = getRequestParams();
        operation = checkThatOperationExists(operation, allowedOperations);
        String oper = getRelativePath();
        String userId = identityProvider.getName();
        logger.debug("Executing " + operation + " on task " + taskId + " by user " + userId );

        TaskCommand<?> cmd = null;

        if ("activate".equalsIgnoreCase(operation)) {
            cmd = new ActivateTaskCommand(taskId, userId);
        } else if ("claim".equalsIgnoreCase(operation)) {
            cmd = new ClaimTaskCommand(taskId, userId);
        } else if ("claimnextavailable".equalsIgnoreCase(operation)) {
            cmd = new ClaimNextAvailableTaskCommand(userId);
        } else if ("complete".equalsIgnoreCase(operation)) {
            Map<String, Object> data = extractMapFromParams(params, operation);
            cmd = new CompleteTaskCommand(taskId, userId, data);
        } else if ("delegate".equalsIgnoreCase(operation)) {
            String targetEntityId = getStringParam("targetEntityId", true, params, oper);
            cmd = new DelegateTaskCommand(taskId, userId, targetEntityId);
        } else if ("exit".equalsIgnoreCase(operation)) {
            cmd = new ExitTaskCommand(taskId, userId);
        } else if ("fail".equalsIgnoreCase(operation)) {
            Map<String, Object> data = extractMapFromParams(params, oper);
            cmd = new FailTaskCommand(taskId, userId, data);
        } else if ("forward".equalsIgnoreCase(operation)) {
            String targetEntityId = getStringParam("targetEntityId", true, params, oper);
            cmd = new ForwardTaskCommand(taskId, userId, targetEntityId);
        } else if ("release".equalsIgnoreCase(operation)) {
            cmd = new ReleaseTaskCommand(taskId, userId);
        } else if ("resume".equalsIgnoreCase(operation)) {
            cmd = new ResumeTaskCommand(taskId, userId);
        } else if ("skip".equalsIgnoreCase(operation)) {
            cmd = new SkipTaskCommand(taskId, userId);
        } else if ("start".equalsIgnoreCase(operation)) {
            cmd = new StartTaskCommand(taskId, userId);
        } else if ("stop".equalsIgnoreCase(operation)) {
            cmd = new StopTaskCommand(taskId, userId);
        } else if ("suspend".equalsIgnoreCase(operation)) {
            cmd = new SuspendTaskCommand(taskId, userId);
        } else if ("nominate".equalsIgnoreCase(operation)) {
            List<OrganizationalEntity> potentialOwners = getOrganizationalEntityListFromParams(params, true, oper);
            cmd = new NominateTaskCommand(taskId, userId, potentialOwners);
        } else {
            throw KieRemoteRestOperationException.badRequest("Unsupported operation: " + oper);
        }

        doRestTaskOperationWithTaskId(taskId, cmd);
        return createCorrectVariant(new JaxbGenericResponse(getRequestUri()), headers);
    }

    private static String checkThatOperationExists(String operation, String[] possibleOperations) {
        for (String oper : possibleOperations) {
            if (oper.equals(operation.trim().toLowerCase())) {
                return oper;
            }
        }
        throw KieRemoteRestOperationException.badRequest("Operation '" + operation + "' is not supported on tasks.");
    }

    @GET
    @Path("/{taskId: [0-9-]+}/content")
    public Response getTaskContentByTaskId(@PathParam("taskId") long taskId) { 
        TaskCommand<?> cmd = new GetTaskCommand(taskId);
        Object result = doRestTaskOperationWithTaskId(taskId, cmd);
        if( result == null ) {
            throw KieRemoteRestOperationException.notFound("Task " + taskId + " could not be found.");
        }
        Task task = ((Task) result);
        long contentId = task.getTaskData().getDocumentContentId();
        JaxbContent content = null;
        if( contentId > -1 ) { 
            cmd = new GetContentCommand(contentId);
            result = processRequestBean.doRestTaskOperation(taskId, task.getTaskData().getDeploymentId(), task.getTaskData().getProcessInstanceId(), task, cmd);
            content = (JaxbContent) result;
        } else { 
            throw KieRemoteRestOperationException.notFound("Content for task " + taskId + " could not be found.");
        }
        return createCorrectVariant(content, headers);
    }

    @GET
    @Path("/{taskId: [0-9-]+}/getFormData")
    public Response getFormData(@PathParam("taskId") long taskId) { 
        
        TaskCommand<?> cmd = new GetTaskCommand(taskId);
        Object result = doRestTaskOperationWithTaskId(taskId, cmd);
        if( result == null ) {
            throw KieRemoteRestOperationException.notFound("Task " + taskId + " could not be found.");
        }
        
        JaxbTask taskJax = (JaxbTask) result;
        TaskData td = taskJax.getTaskData();
        String deploymentId = td.getDeploymentId();
        String processId = td.getProcessId();
        String formName=taskJax.getFormName();

        ProcessDefinition processAssetDescList = runtimeDataService.getProcessesByDeploymentIdProcessId(deploymentId, processId);
        JaxbProcessDefinition jaxbProcDef = convertProcAssetDescToJaxbProcDef(processAssetDescList);
        String forms=jaxbProcDef.getForms().get(formName+"-taskform.form");

        Map<String,String>ret=new HashMap<String, String>();
        ret.put("deploiment", deploymentId);
        ret.put("process-id", processId);
        ret.put("formName", formName);
        ret.put("form", forms);

        return createCorrectVariant(ret, headers);
    }

    @GET
    @Path("/{taskId: [0-9-]+}/showTaskForm")
    public Response getTaskFormByTaskId(@PathParam("taskId") long taskId) {
        TaskCommand<?> cmd = new GetTaskCommand(taskId);
        Object result = doRestTaskOperationWithTaskId(taskId, cmd);

        if (result != null) {
            String opener = "";

            List<String> openers = headers.getRequestHeader("host");
            if (openers.size() == 1) {
                opener = openers.get(0);
            }
            String formUrl = formURLGenerator.generateFormTaskURL(getBaseUri(), taskId, opener);
            if (!StringUtils.isEmpty(formUrl)) {
                JaxbTaskFormResponse response = new JaxbTaskFormResponse(formUrl, getRequestUri());
                return createCorrectVariant(response, headers);
            }
        }
        throw KieRemoteRestOperationException.notFound("Task " + taskId + " could not be found.");
    }

    @GET
    @Path("/content/{contentId: [0-9-]+}")
    public Response getTaskContentByContentId(@PathParam("contentId") long contentId) { 
        TaskCommand<?> cmd = new GetContentCommand(contentId);
        cmd.setUserId(identityProvider.getName());
        JaxbContent content = (JaxbContent) doRestTaskOperation(cmd);
        if( content == null ) { 
            throw KieRemoteRestOperationException.notFound("Content " + contentId + " could not be found.");
        }
        return createCorrectVariant(new JaxbContent(content), headers);
    }

    @POST
    @Path("/history/bam/clear")
    public Response clearTaskBamHistory() { 
        doRestTaskOperation(new DeleteBAMTaskSummariesCommand());
        return createCorrectVariant(new JaxbGenericResponse(getRelativePath()), headers);
    }


}
