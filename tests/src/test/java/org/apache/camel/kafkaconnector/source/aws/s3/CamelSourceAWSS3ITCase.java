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

package org.apache.camel.kafkaconnector.source.aws.s3;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import com.amazonaws.services.s3.AmazonS3;
import org.apache.camel.kafkaconnector.AbstractKafkaTest;
import org.apache.camel.kafkaconnector.ConnectorPropertyFactory;
import org.apache.camel.kafkaconnector.TestCommon;
import org.apache.camel.kafkaconnector.clients.kafka.KafkaClient;
import org.apache.camel.kafkaconnector.services.aws.AWSService;
import org.apache.camel.kafkaconnector.services.aws.AWSServiceFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
public class CamelSourceAWSS3ITCase extends AbstractKafkaTest {
    @RegisterExtension
    public static AWSService<AmazonS3> service = AWSServiceFactory.createS3Service();

    private static final Logger LOG = LoggerFactory.getLogger(CamelSourceAWSS3ITCase.class);

    private AmazonS3 awsS3Client;

    private volatile int received;
    private final int expect = 10;

    @BeforeEach
    public void setUp() {
        awsS3Client = service.getClient();
    }

    private boolean checkRecord(ConsumerRecord<String, String> record) {
        LOG.debug("Received: {}", record.value());
        received++;

        if (received == expect) {
            return false;
        }

        return true;
    }

    @Test
    @Timeout(180)
    public void testBasicSendReceive() throws ExecutionException, InterruptedException {
        Properties properties = service.getConnectionProperties();

        ConnectorPropertyFactory testProperties = new CamelAWSS3PropertyFactory(1,
                TestCommon.getDefaultTestTopic(this.getClass()), TestCommon.DEFAULT_S3_BUCKET, properties);

        getKafkaConnectService().initializeConnector(testProperties);

        awsS3Client.createBucket(TestCommon.DEFAULT_S3_BUCKET);

        LOG.debug("Putting S3 objects");
        for (int i = 0; i < expect; i++) {
            String name = "file" + i + ".test";
            String file = this.getClass().getResource(name).getFile();

            LOG.trace("Putting file " + file);
            awsS3Client.putObject(TestCommon.DEFAULT_S3_BUCKET, name, new File(file));
        }
        LOG.debug("Done putting S3S objects");

        LOG.debug("Creating the consumer ...");
        KafkaClient<String, String> kafkaClient = new KafkaClient<>(getKafkaService().getBootstrapServers());
        kafkaClient.consume(TestCommon.getDefaultTestTopic(this.getClass()), this::checkRecord);
        LOG.debug("Created the consumer ...");

        assertEquals(received, expect, "Didn't process the expected amount of messages");
    }
}
