package com.nsauto.leaderlock;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.leader-lock")
public class LeaderLockProperties {

    private String redisUrl = "redis://localhost:6379";
    private String deploymentMode = "single";
    private List<String> nodeAddresses = new ArrayList<>();
    private int clusterScanInterval = 5_000;
    private boolean tlsEnabled;
    private String truststorePath = "";
    private String truststorePassword = "";
    private String keystorePath = "";
    private String keystorePassword = "";
    private String keystoreType = "JKS";
    private boolean sslEndpointIdentification = true;
    private String username = "";
    private String password = "";
    private String key = "app:leader-lock";
    private Duration retryDelay = Duration.ofSeconds(5);
    private Duration confirmationInterval = Duration.ofSeconds(3);
    private Duration shutdownTimeout = Duration.ofSeconds(5);
    private String instanceId = "";

    public String getRedisUrl() {
        return redisUrl;
    }

    public void setRedisUrl(String redisUrl) {
        this.redisUrl = redisUrl;
    }

    public String getDeploymentMode() {
        return deploymentMode;
    }

    public void setDeploymentMode(String deploymentMode) {
        this.deploymentMode = deploymentMode;
    }

    public List<String> getNodeAddresses() {
        return nodeAddresses;
    }

    public void setNodeAddresses(List<String> nodeAddresses) {
        this.nodeAddresses = nodeAddresses;
    }

    public int getClusterScanInterval() {
        return clusterScanInterval;
    }

    public void setClusterScanInterval(int clusterScanInterval) {
        this.clusterScanInterval = clusterScanInterval;
    }

    public boolean isTlsEnabled() {
        return tlsEnabled;
    }

    public void setTlsEnabled(boolean tlsEnabled) {
        this.tlsEnabled = tlsEnabled;
    }

    public String getTruststorePath() {
        return truststorePath;
    }

    public void setTruststorePath(String truststorePath) {
        this.truststorePath = truststorePath;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    public void setTruststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
    }

    public String getKeystorePath() {
        return keystorePath;
    }

    public void setKeystorePath(String keystorePath) {
        this.keystorePath = keystorePath;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getKeystoreType() {
        return keystoreType;
    }

    public void setKeystoreType(String keystoreType) {
        this.keystoreType = keystoreType;
    }

    public boolean isSslEndpointIdentification() {
        return sslEndpointIdentification;
    }

    public void setSslEndpointIdentification(boolean sslEndpointIdentification) {
        this.sslEndpointIdentification = sslEndpointIdentification;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(Duration retryDelay) {
        this.retryDelay = retryDelay;
    }

    public Duration getConfirmationInterval() {
        return confirmationInterval;
    }

    public void setConfirmationInterval(Duration confirmationInterval) {
        this.confirmationInterval = confirmationInterval;
    }

    public Duration getShutdownTimeout() {
        return shutdownTimeout;
    }

    public void setShutdownTimeout(Duration shutdownTimeout) {
        this.shutdownTimeout = shutdownTimeout;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
}