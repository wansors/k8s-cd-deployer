package com.github.wansors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.json.bind.JsonbBuilder;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class PubSubApplication {

    private static final Logger LOG = Logger.getLogger(PubSubApplication.class);

    @ConfigProperty(name = "k8s-cd-deployer.namespace.regex", defaultValue = ".*-int$")
    String namespaceRegex;

    @ConfigProperty(name = "k8s-cd-deployer.image.regex", defaultValue = ".*:INT$")
    String validImageRegex;

    @ConfigProperty(name = "k8s-cd-deployer.pubsub.subscription.name", defaultValue = "gcr-pull")
    String subscriptionName;

    /**
     * Inject the projectId property from application.properties
     */
    @ConfigProperty(name = "k8s-cd-deployer.pubsub.project-id")
    String projectId;

    private final KubernetesClient kubernetesClient;

    public PubSubApplication(KubernetesClient kubernetesClient) {
	this.kubernetesClient = kubernetesClient;
    }

    private Subscriber subscriber;

    void onStart(@Observes StartupEvent ev) {
	LOG.info("The application is starting...");
	// Init topic and subscription, the topic must have been created before
	var subscriptionNameObject = ProjectSubscriptionName.of(this.projectId, this.subscriptionName);

	// Subscribe to PubSub
	MessageReceiver receiver = (message, consumer) -> {
	    this.process(message);
	    consumer.ack();
	};
	this.subscriber = Subscriber.newBuilder(subscriptionNameObject, receiver).build();

	this.subscriber.startAsync().awaitRunning();
    }

    private void process(PubsubMessage message) {

	// Deserialize back
	var json = JsonbBuilder.create().fromJson(message.getData().toStringUtf8(), GcrMessageJson.class);
	if ("INSERT".equals(json.getAction())) {
	    LOG.debugv("Processing TAG {0}", json.getTag());
	    // Check send restart call for image ID
	    this.restartDeployments(json.getTag());

	} else {
	    LOG.debugv("Ignoring message type {0}", json.getAction());
	}

    }

    public void restartDeployments(String tag) {

	if (!tag.matches(this.validImageRegex)) {
	    LOG.debugv("Restart disabled for TAG {0}", tag);
	    return;
	}
	LOG.infov("Restart enabled for TAG");

	for (Deployment d : this.kubernetesClient.apps().deployments().inAnyNamespace().list().getItems()) {
	    if (!d.getMetadata().getNamespace().matches(this.namespaceRegex) || !d.getSpec().getTemplate().getSpec().getContainers().get(0).getImage().equals(tag)) {
		continue;
	    }

	    this.restartSingleDeployment(d.getMetadata().getNamespace(), d.getMetadata().getName());

	}

    }

    private void restartSingleDeployment(String namespace, String deploymentName) {
	// restart deployment
	LOG.infov("Performing restart on {0}:{1}", namespace, deploymentName);
	this.kubernetesClient.apps().deployments().inNamespace(namespace).withName(deploymentName).rolling().restart();

    }

}
