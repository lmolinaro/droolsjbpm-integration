package org.kie.remote.services.rest;

import java.util.ArrayList;
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
import javax.ws.rs.core.Response.Status;

import org.jbpm.services.api.model.DeploymentUnit;
import org.kie.internal.runtime.conf.DeploymentDescriptor;
import org.kie.services.client.serialization.jaxb.impl.deploy.JaxbDeploymentDescriptor;
import org.kie.services.client.serialization.jaxb.impl.deploy.JaxbDeploymentJobResult;
import org.kie.services.client.serialization.jaxb.impl.deploy.JaxbDeploymentUnit;
import org.kie.services.client.serialization.jaxb.impl.process.JaxbProcessDefinition;
import org.kie.services.client.serialization.jaxb.impl.process.JaxbProcessDefinitionList;
import org.kie.services.client.serialization.jaxb.rest.JaxbGenericResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This REST resource is responsible for retrieving information about and managing deployment units. 
 */
@RequestScoped
@Path("/deployment/{deploymentId: [\\w\\.-]+(:[\\w\\.-]+){2,2}(:[\\w\\.-]*){0,2}}")
public class DeploymentResourceImpl extends ResourceBase {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentResourceImpl.class);

    /* REST information */

    @Context
    private HttpHeaders headers;

    @PathParam("deploymentId")
    private String deploymentId;

    /* Deployment operations */

    @Inject 
    private DeployResourceBase deployResourceBase;

    // REST operations -----------------------------------------------------------------------------------------------------------

    /**
     * Retrieve the status of the {@link DeploymentUnit} specified in the URL.
     * 
     * @return A {@link JaxbDeploymentUnit} instance
     */
    @GET
    public Response getConfig() { 
        JaxbDeploymentUnit jaxbDepUnit = deployResourceBase.determineStatus(deploymentId, true);
        logger.debug("Returning deployment unit information for " + deploymentId);
        return createCorrectVariant(jaxbDepUnit, headers);
    }

    /**
     * Queues a request to deploy the given deployment unit. If the deployment already exist, this
     * operation will fail.
     * 
     * @param deployDescriptor An optional {@link DeploymentDescriptor} instance specifying additional information about how
     * the deployment unit should be deployed.
     * @return A {@link JaxbDeploymentJobResult} instance with the initial status of the job
     */
    @POST
    @Path("/deploy")
    public Response deploy(JaxbDeploymentDescriptor deployDescriptor) {
        // parse request/options 
        Map<String, String []> params = getRequestParams();
        String oper = getRelativePath();
        String strategy = getStringParam("strategy", false, params, oper);
        String mergeMode = getStringParam("mergemode", false, params, oper);

        // schedule deployment
        JaxbDeploymentJobResult jobResult = deployResourceBase.submitDeployJob(deploymentId, strategy, mergeMode, deployDescriptor);
        return createCorrectVariant(jobResult, headers, Status.ACCEPTED);
    }

    /**
     * Queues a request to undeploy the deployment unit specified in the URL
     * 
     * @return A {@link JaxbDeploymentJobResult} instance with the initial status of the job
     */
    @POST
    @Path("/undeploy")
    public Response undeploy() { 
        JaxbDeploymentJobResult jobResult = deployResourceBase.submitUndeployJob(deploymentId);
        return createCorrectVariant(jobResult, headers, Status.ACCEPTED);
    }


    /**
     * Returns a list of the first ten process definitions for the specified deployment.
     * @return A {@link JaxbProcessDefinitionList} instance
     */
    @GET
    @Path("/process")
    public Response listFirstTenProcessDefinitions() { 
        int [] pageInfo = {0,10};
        int maxNumResults = 10; 
        JaxbProcessDefinitionList jaxbProcDefList  = new JaxbProcessDefinitionList();
        deployResourceBase.fillProcessDefinitionList(deploymentId, pageInfo, maxNumResults, jaxbProcDefList.getProcessDefinitionList());
        JaxbProcessDefinitionList resultList = paginateAndCreateResult(pageInfo, jaxbProcDefList.getProcessDefinitionList(), new JaxbProcessDefinitionList());
        
        JaxbProcessDefinitionList resultList2 = removeFormsIfNeeded(resultList);
        
        return createCorrectVariant(resultList2, headers);
    }

    private JaxbProcessDefinitionList removeFormsIfNeeded(JaxbProcessDefinitionList resultList) {
        JaxbProcessDefinitionList resultList2 = null;
        Map<String, String[]> params = getRequestParams();
        logger.info(params.get("minimal").toString());
        boolean expressionLogic = params.containsKey("minimal") && params.get("minimal")[0].equals("true");
        logger.info(expressionLogic+"");
        if(expressionLogic){
            resultList2=new JaxbProcessDefinitionList();
            resultList2.setPageNumber(resultList.getPageNumber());
            resultList2.setPageSize(resultList.getPageSize());
            List<JaxbProcessDefinition> processDefinitionList = resultList.getProcessDefinitionList();
            List<JaxbProcessDefinition> processDefinitionList2 = new ArrayList<JaxbProcessDefinition>();
            for (JaxbProcessDefinition jpd : processDefinitionList) {
                JaxbProcessDefinition jd=new JaxbProcessDefinition();
                jd.setDeploymentId(jpd.getDeploymentId());
                jd.setId(jpd.getId());
                jd.setName(jpd.getName());
                jd.setPackageName(jpd.getPackageName());
                jd.setVersion(jpd.getVersion());
                processDefinitionList2.add(jd);
            }
            resultList2.setProcessDefinitionList(processDefinitionList2);
        }else{
            resultList2=resultList;
        }
        return resultList2;
    }

    /**
     * Returns a list of process definitions for the specified deployment.
     * @return A {@link JaxbProcessDefinitionList} instance
     */
    @GET
    @Path("/processes")
    public Response listProcessDefinitions() { 
        String oper = getRelativePath();
        Map<String, String[]> params = getRequestParams();
        int [] pageInfo = getPageNumAndPageSize(params, oper);
        int maxNumResults = getMaxNumResultsNeeded(pageInfo); 

        JaxbProcessDefinitionList jaxbProcDefList  = new JaxbProcessDefinitionList();
        deployResourceBase.fillProcessDefinitionList(deploymentId, pageInfo, maxNumResults, jaxbProcDefList.getProcessDefinitionList());
        JaxbProcessDefinitionList resultList 
        = paginateAndCreateResult(pageInfo, jaxbProcDefList.getProcessDefinitionList(), new JaxbProcessDefinitionList());
       
        JaxbProcessDefinitionList resultList2 = removeFormsIfNeeded(resultList);
        
        return createCorrectVariant(resultList2, headers);
    }

    @POST
    @Path("/activate")
    public Response activate() {
        deployResourceBase.activate(deploymentId);
        return createCorrectVariant(new JaxbGenericResponse(getRequestUri()), headers);

    }

    @POST
    @Path("/deactivate")
    public Response deactivate() {
        deployResourceBase.deactivate(deploymentId);
        return createCorrectVariant(new JaxbGenericResponse(getRequestUri()), headers);

    }
}
