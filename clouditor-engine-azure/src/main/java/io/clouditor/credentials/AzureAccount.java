/*
 * Copyright 2016-2019 Fraunhofer AISEC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *            $$\                           $$\ $$\   $$\
 *            $$ |                          $$ |\__|  $$ |
 *   $$$$$$$\ $$ | $$$$$$\  $$\   $$\  $$$$$$$ |$$\ $$$$$$\    $$$$$$\   $$$$$$\
 *  $$  _____|$$ |$$  __$$\ $$ |  $$ |$$  __$$ |$$ |\_$$  _|  $$  __$$\ $$  __$$\
 *  $$ /      $$ |$$ /  $$ |$$ |  $$ |$$ /  $$ |$$ |  $$ |    $$ /  $$ |$$ |  \__|
 *  $$ |      $$ |$$ |  $$ |$$ |  $$ |$$ |  $$ |$$ |  $$ |$$\ $$ |  $$ |$$ |
 *  \$$$$$$\  $$ |\$$$$$   |\$$$$$   |\$$$$$$  |$$ |  \$$$   |\$$$$$   |$$ |
 *   \_______|\__| \______/  \______/  \_______|\__|   \____/  \______/ \__|
 *
 * This file is part of Clouditor Community Edition.
 */

package io.clouditor.credentials;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.credentials.AzureCliCredentials;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.Azure;
import java.io.File;
import java.io.IOException;

@JsonTypeName("Azure")
public class AzureAccount extends CloudAccount<AzureTokenCredentials> {

  @JsonProperty private String clientId;
  @JsonProperty private String tenantId;
  @JsonProperty private String domain;
  // TODO: might be needed again if an account has multiple subscriptions to find the correct one
  // @JsonProperty private String subscriptionId;
  @JsonProperty private String clientSecret;

  public static AzureAccount discover() throws IOException {
    var account = new AzureAccount();

    // fetch credentials from default credential chain
    var credentials = defaultCredentialProviderChain();

    var azure = Azure.authenticate(credentials).withDefaultSubscription();

    account.setAccountId(azure.getCurrentSubscription().displayName());
    account.setAutoDiscovered(true);
    account.setDomain(credentials.domain());

    return account;
  }

  private static AzureTokenCredentials defaultCredentialProviderChain() throws IOException {
    // check if the default credentials-file exists
    var credentialsFile = new File(defaultAuthFile());

    if (credentialsFile.exists()) {
      LOGGER.info("Using default credentials file {}", credentialsFile);
      return ApplicationTokenCredentials.fromFile(credentialsFile);
    } else {
      // otherwise, use default locations
      LOGGER.info("Did not find default credentials. Trying to use AzureCLI credentials instead.");
      return AzureCliCredentials.create();
    }
  }

  @Override
  public void validate() throws IOException {
    var credentials = this.resolveCredentials();

    try {
      var azure = Azure.authenticate(credentials).withDefaultSubscription();

      this.setAccountId(azure.getCurrentSubscription().displayName());
    } catch (RuntimeException ex) {
      throw new IOException(ex.getCause());
    }
  }

  public AzureTokenCredentials resolveCredentials() throws IOException {
    if (this.isAutoDiscovered()) {
      return AzureAccount.defaultCredentialProviderChain();
    } else {
      return new ApplicationTokenCredentials(
          clientId, tenantId, clientSecret, AzureEnvironment.AZURE);
    }
  }

  private static String defaultAuthFile() {
    return System.getenv()
        .getOrDefault(
            "AZURE_AUTH_LOCATION", System.getProperty("user.home") + "/.azure/clouditor.azureauth");
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  @JsonProperty
  public String getAuthFile() {
    return defaultAuthFile();
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }
}
