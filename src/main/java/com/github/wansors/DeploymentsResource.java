package com.github.wansors;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;

@Path("/deployments")
public class DeploymentsResource {

    private final KubernetesClient kubernetesClient;

    @Inject
    PubSubApplication pubSubApplication;

    public DeploymentsResource(KubernetesClient kubernetesClient) {
	this.kubernetesClient = kubernetesClient;
    }

    @GET
    @Produces("application/json")
    @Path("/{namespace}")
    public List<Pod> pods(@PathParam("namespace") String namespace) {
	return this.kubernetesClient.pods().inNamespace(namespace).list().getItems();
    }

    /**
     * Restarts all deployments that have the current tag
     *
     * @param tags
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/restart/{tag:.*}")
    public Response restartDeployment(@PathParam("tag") String tag) {

	this.pubSubApplication.restartDeployments(tag);
	return Response.accepted().build();

    }

    @GET
    @Produces("application/json")
    @Path("/all")
    public List<DeploymentSummaryJson> deployments() {
	// filter namespaces by text var
	List<DeploymentSummaryJson> list = new ArrayList<>();
	for (Deployment d : this.kubernetesClient.apps().deployments().inAnyNamespace().list().getItems()) {
	    DeploymentSummaryJson ds = new DeploymentSummaryJson();
	    ds.setName(d.getMetadata().getName());
	    ds.setNamespace(d.getMetadata().getNamespace());
	    ds.setImage(d.getSpec().getTemplate().getSpec().getContainers().get(0).getImage());
	    list.add(ds);

	}
	return list;
    }

    @GET
    @Produces("application/json")
    @Path("/raw")
    public List<Deployment> deploymentsRaw() {
	return this.kubernetesClient.apps().deployments().inAnyNamespace().list().getItems();
    }
}