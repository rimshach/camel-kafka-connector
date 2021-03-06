/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.camel.kafkaconnector.services.aws;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.apache.camel.kafkaconnector.clients.aws.sqs.AWSSQSClient;
import org.testcontainers.containers.localstack.LocalStackContainer;

public class AWSSQSLocalContainerService extends AWSLocalContainerService<AWSSQSClient> {

    public AWSSQSLocalContainerService() {
        super(LocalStackContainer.Service.SQS);
    }

    @Override
    public String getServiceEndpoint() {
        return super.getServiceEndpoint(LocalStackContainer.Service.SQS);
    }

    @Override
    public String getAmazonHost() {
        final int sqsPort = 4576;

        return super.getAmazonHost(sqsPort);
    }

    @Override
    public AWSSQSClient getClient() {
        AmazonSQS sqs = AmazonSQSClientBuilder
                .standard()
                .withEndpointConfiguration(getContainer()
                        .getEndpointConfiguration(LocalStackContainer.Service.SQS))
                .withCredentials(getContainer().getDefaultCredentialsProvider())
                .build();

        return new AWSSQSClient(sqs);
    }
}
