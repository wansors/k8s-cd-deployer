package com.github.wansors;

public class DeploymentSummaryJson {
    private String namespace;
    private String name;
    private String image;

    public String getNamespace() {
	return this.namespace;
    }

    public void setNamespace(String namespace) {
	this.namespace = namespace;
    }

    public String getName() {
	return this.name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public String getImage() {
	return this.image;
    }

    public void setImage(String image) {
	this.image = image;
    }

}
