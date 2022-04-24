# k8s-cd-deployer Project

This project uses Quarkus, the Supersonic Subatomic Java Framework.

THis project  subscribes to a subscription on topic gcr on GCP. Where for each push, tag or delete creates a notification.

Then restart the deployments that match a regex on the imageName and the contextName.


Is necesseray to definbe Google credentials for pubsub access and permission to the k8s cluster.

export GOOGLE_APPLICATION_CREDENTIALS=credentials.json
