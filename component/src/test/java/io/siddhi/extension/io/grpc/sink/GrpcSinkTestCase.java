/*
 * Copyright (c)  2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.siddhi.extension.io.grpc.sink;

import io.siddhi.core.SiddhiAppRuntime;
import io.siddhi.core.SiddhiManager;
import io.siddhi.core.stream.input.InputHandler;
import io.siddhi.core.util.persistence.InMemoryPersistenceStore;
import io.siddhi.extension.io.grpc.utils.TestAppender;
import io.siddhi.extension.io.grpc.utils.TestServer;
import io.siddhi.query.api.exception.SiddhiAppValidationException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Test cases for grpc-sink in default way.
 */
public class GrpcSinkTestCase {
    private static final Logger log = (Logger) LogManager.getLogger(GrpcSinkTestCase.class);
    private TestServer server = new TestServer(5003);

    @BeforeTest
    public void init() throws IOException {
        server.start();
    }

    @AfterTest
    public void stop() throws InterruptedException {
        server.stop();
    }

    @Test
    public void testCaseToCallConsumeWithSimpleRequest() throws Exception {
        log.info("Test case to call consume");
        TestAppender appender = new TestAppender("TestAppender", null);
        final Logger logger = (Logger) LogManager.getRootLogger();
        logger.setLevel(Level.ALL);
        logger.addAppender(appender);
        appender.start();
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = ""
                + "@sink(type='grpc', publisher.url = 'grpc://localhost:5003/org.wso2.grpc.EventService/consume', " +
                "@map(type='json', @payload('{{message}}'))) " +
                "define stream FooStream (message String);";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition);
        InputHandler fooStream = siddhiAppRuntime.getInputHandler("FooStream");

        siddhiAppRuntime.start();
        fooStream.send(new Object[]{"Request 1"});
        Thread.sleep(1000);
        siddhiAppRuntime.shutdown();

        Assert.assertTrue(((TestAppender) logger.getAppenders().
                get("TestAppender")).getMessages().contains("Server consume hit with payload = " +
                "[Request 1] and Headers = {{}}"));
        logger.removeAppender(appender);
    }

    @Test
    public void testCaseToCallConsumeWithTwoRequests() throws Exception {
        log.info("Test case to call consume with 2 requests");
        TestAppender appender = new TestAppender("TestAppender", null);
        final Logger logger = (Logger) LogManager.getRootLogger();
        logger.setLevel(Level.ALL);
        logger.addAppender(appender);
        appender.start();
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = ""
                + "@sink(type='grpc', publisher.url = 'grpc://localhost:5003/org.wso2.grpc.EventService/consume', " +
                "@map(type='json', @payload('{{message}}'))) " +
                "define stream FooStream (message String);";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition);
        InputHandler fooStream = siddhiAppRuntime.getInputHandler("FooStream");

        siddhiAppRuntime.start();
        fooStream.send(new Object[]{"Request 1"});
        fooStream.send(new Object[]{"Request 2"});
        Thread.sleep(1000);
        siddhiAppRuntime.shutdown();

        Assert.assertTrue(((TestAppender) logger.getAppenders().
                get("TestAppender")).getMessages().contains("Server consume hit with payload = " +
                "[Request 1] and Headers = {{}}"));
        Assert.assertTrue(((TestAppender) logger.getAppenders().
                get("TestAppender")).getMessages().contains("Server consume hit with payload = " +
                "[Request 2] and Headers = {{}}"));
        logger.removeAppender(appender);
    }

    @Test
    public void testWithHeader() throws Exception {
        log.info("Test case to call consume with headers");
        TestAppender appender = new TestAppender("TestAppender", null);
        final Logger logger = (Logger) LogManager.getRootLogger();
        logger.setLevel(Level.ALL);
        logger.addAppender(appender);
        appender.start();
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = ""
                + "@sink(type='grpc', " +
                "publisher.url = 'grpc://localhost:5003/org.wso2.grpc.EventService/consume', " +
                "headers='{{headers}}', " +
                "@map(type='json', @payload('{{message}}'))) " +
                "define stream FooStream (message String, headers String);";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition);
        InputHandler fooStream = siddhiAppRuntime.getInputHandler("FooStream");

        siddhiAppRuntime.start();
        fooStream.send(new Object[]{"Request 1", "'Name:John','Age:23','Content-Type:text'"});
        fooStream.send(new Object[]{"Request 2", "'Name:Nash','Age:54','Content-Type:json'"});
        Thread.sleep(1000);
        siddhiAppRuntime.shutdown();

        Assert.assertTrue(((TestAppender) logger.getAppenders().
                get("TestAppender")).getMessages().contains("Server consume hit with payload = [Request 1] " +
                "and Headers = {{Name=John, Age=23, Content-Type=text}}"));
        Assert.assertTrue(((TestAppender) logger.getAppenders().
                get("TestAppender")).getMessages().contains("Server consume hit with payload = [Request 2] " +
                "and Headers = {{Name=Nash, Age=54, Content-Type=json}}"));
        logger.removeAppender(appender);
    }

    @Test
    public void testCaseWithWrongProtocol() throws Exception {
        log.info("Test case to call consume");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = ""
                + "@sink(type='grpc', publisher.url = 'grc://localhost:5003/org.wso2.grpc.EventService/consume', " +
                "@map(type='json', @payload('{{message}}'))) " +
                "define stream FooStream (message String);";

        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition);
        } catch (SiddhiAppValidationException e) {
            Assert.assertTrue(e.getMessage().contains("The url must begin with \"grpc\" for all grpc sinks"));
        }
    }

    @Test
    public void testCaseWithMalformedURL1() throws Exception {
        log.info("Test case to call consume");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = ""
                + "@sink(type='grpc', publisher.url = 'grpc://localhost:5003', " +
                "@map(type='json', @payload('{{message}}'))) " +
                "define stream FooStream (message String);";

        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition);
        } catch (SiddhiAppValidationException e) {
            Assert.assertTrue(e.getMessage().contains("URL not properly given. Expected format is " +
                    "`grpc://0.0.0.0:9763/<serviceName>/<methodName>` or `grpc://0.0.0.0:9763/<sequenceName>` but " +
                    "the provided url is grpc://localhost:5003. "));
        }
    }

    @Test
    public void testCaseWithMalformedURL2() throws Exception {
        log.info("Test case to call consume");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = ""
                + "@sink(type='grpc', publisher.url = 'grpc:dfasf', " +
                "@map(type='json', @payload('{{message}}'))) " +
                "define stream FooStream (message String);";

        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition);
        } catch (SiddhiAppValidationException e) {
            Assert.assertTrue(e.getMessage().contains("URL not properly given. Expected format is " +
                    "`grpc://0.0.0.0:9763/<serviceName>/<methodName>` or `grpc://0.0.0.0:9763/<sequenceName>` but " +
                    "the provided url is grpc:dfasf. "));
        }
    }

    @Test
    public void testCaseWithShortURL() throws Exception {
        log.info("Test case to call consume");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = ""
                + "@sink(type='grpc', publisher.url = 'grpc://localhost:5003/mySeq', " +
                "@map(type='json', @payload('{{message}}'))) " +
                "define stream FooStream (message String);";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition);
    }

    @Test
    public void testCaseWithXMLMapper() throws Exception {
        log.info("Test case to call consume");
        TestAppender appender = new TestAppender("TestAppender", null);
        final Logger logger = (Logger) LogManager.getRootLogger();
        logger.setLevel(Level.ALL);
        logger.addAppender(appender);
        appender.start();
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = ""
                + "@sink(type='grpc', publisher.url = 'grpc://localhost:5003/org.wso2.grpc.EventService/consume', " +
                "@map(type='xml', @payload('{{message}}'))) " +
                "define stream FooStream (message String);";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition);
        InputHandler fooStream = siddhiAppRuntime.getInputHandler("FooStream");

        siddhiAppRuntime.start();
        fooStream.send(new Object[]{"Request 1"});
        Thread.sleep(1000);
        siddhiAppRuntime.shutdown();

        Assert.assertTrue(((TestAppender) logger.getAppenders().
                get("TestAppender")).getMessages().contains("Server consume hit with payload = Request 1 " +
                "and Headers = {{}}"));
        logger.removeAppender(appender);
    }

    @Test
    public void testCaseWithSequenceName() throws Exception {
        log.info("Test case to call consume");
        TestAppender appender = new TestAppender("TestAppender", null);
        final Logger logger = (Logger) LogManager.getRootLogger();
        logger.setLevel(Level.ALL);
        logger.addAppender(appender);
        appender.start();
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = ""
                + "@sink(type='grpc', " +
                "publisher.url = 'grpc://localhost:5003/org.wso2.grpc.EventService/consume/mySeq', " +
                "@map(type='xml', @payload('{{message}}'))) " +
                "define stream FooStream (message String);";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition);
        InputHandler fooStream = siddhiAppRuntime.getInputHandler("FooStream");

        siddhiAppRuntime.start();
        fooStream.send(new Object[]{"Request 1"});
        Thread.sleep(1000);
        siddhiAppRuntime.shutdown();

        Assert.assertTrue(((TestAppender) logger.getAppenders().
                get("TestAppender")).getMessages().contains("Server consume hit with payload = " +
                "Request 1 and Headers = " + "{{sequence=mySeq}}"));
        logger.removeAppender(appender);
    }

    @Test
    public void testWithHeaderAndSequenceName() throws Exception {
        log.info("Test case to call consume with headers");
        TestAppender appender = new TestAppender("TestAppender", null);
        final Logger logger = (Logger) LogManager.getRootLogger();
        logger.setLevel(Level.ALL);
        logger.addAppender(appender);
        appender.start();
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = ""
                + "@sink(type='grpc', " +
                "publisher.url = 'grpc://localhost:5003/org.wso2.grpc.EventService/consume/mySeq', " +
                "headers='{{headers}}', " +
                "@map(type='json', @payload('{{message}}'))) " +
                "define stream FooStream (message String, headers String);";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition);
        InputHandler fooStream = siddhiAppRuntime.getInputHandler("FooStream");

        siddhiAppRuntime.start();
        fooStream.send(new Object[]{"Request 1", "'Name:John','Age:23','Content-Type:text'"});
        fooStream.send(new Object[]{"Request 2", "'Name:Nash','Age:54','Content-Type:json'"});
        Thread.sleep(1000);
        siddhiAppRuntime.shutdown();

        Assert.assertTrue(((TestAppender) logger.getAppenders().
                get("TestAppender")).getMessages().contains("Server consume hit with payload = " +
                "[Request 1] and Headers = {{Name=John, Age=23, Content-Type=text, sequence=mySeq}}"));
        Assert.assertTrue(((TestAppender) logger.getAppenders().
                get("TestAppender")).getMessages().contains("Server consume hit with payload = [Request 2] " +
                "and Headers = {{Name=Nash, Age=54, Content-Type=json, sequence=mySeq}}"));
        logger.removeAppender(appender);
    }

    @Test
    public void testWithMetaData() throws Exception {
        log.info("Test case to call consume with headers");
        TestAppender appender = new TestAppender("TestAppender", null);
        final Logger logger = (Logger) LogManager.getRootLogger();
        logger.setLevel(Level.ALL);
        logger.addAppender(appender);
        appender.start();
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = ""
                + "@sink(type='grpc', " +
                "publisher.url = 'grpc://localhost:5003/org.wso2.grpc.EventService/consume', " +
                "metadata = \"'Name:John','Age:23','Content-Type:text'\", " +
                "@map(type='json')) " +
                "define stream FooStream (message String);";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition);
        InputHandler fooStream = siddhiAppRuntime.getInputHandler("FooStream");

        siddhiAppRuntime.start();
        fooStream.send(new Object[]{"Request 1"});
        Thread.sleep(1000);
        siddhiAppRuntime.shutdown();

        Assert.assertTrue(((TestAppender) logger.getAppenders().
                get("TestAppender")).getMessages().contains("Server consume hit with payload = " +
                "[{\"event\":{\"message\":\"Request 1\"}}] and Headers = {{}}"));
        Assert.assertTrue(((TestAppender) logger.getAppenders().
                get("TestAppender")).getMessages().contains("Metadata received: name: John"));
        Assert.assertTrue(((TestAppender) logger.getAppenders().
                get("TestAppender")).getMessages().contains("Metadata received: age: 23"));
        logger.removeAppender(appender);
    }

    @Test
    public void testCaseFailingWithUnavailableServer() throws Exception {
        log.info("Test case to call consume");
        TestAppender appender = new TestAppender("TestAppender", null);
        final Logger logger = (Logger) LogManager.getRootLogger();
        logger.setLevel(Level.ALL);
        logger.addAppender(appender);
        appender.start();
        SiddhiManager siddhiManager = new SiddhiManager();
        server.stop();

        String inStreamDefinition = ""
                + "@sink(type='grpc', publisher.url = 'grpc://localhost:5003/org.wso2.grpc.EventService/consume', " +
                "@map(type='json', @payload('{{message}}'))) " +
                "define stream FooStream (message String);";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition);
        InputHandler fooStream = siddhiAppRuntime.getInputHandler("FooStream");

        siddhiAppRuntime.start();
        fooStream.send(new Object[]{"Request 1"});
        Thread.sleep(1000);
        siddhiAppRuntime.shutdown();
        server.start();

        Assert.assertTrue(((TestAppender) logger.getAppenders().
                get("TestAppender")).getMessages().contains("UNAVAILABLE: io exception caused by UNAVAILABLE: " +
                "io exception"));
        logger.removeAppender(appender);
    }

    @Test
    public void testCaseWithSiddhiAppShutdown() throws Exception {
        log.info("Test case to call consume with 2 requests");
        TestAppender appender = new TestAppender("TestAppender", null);
        final Logger logger = (Logger) LogManager.getRootLogger();
        logger.setLevel(Level.ALL);
        logger.addAppender(appender);
        appender.start();
        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiManager.setPersistenceStore(new InMemoryPersistenceStore());

        String inStreamDefinition = ""
                + "@sink(type='grpc', publisher.url = 'grpc://localhost:5003/org.wso2.grpc.EventService/consume', " +
                "@map(type='json', @payload('{{message}}'))) " +
                "define stream FooStream (message String);";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition);
        InputHandler fooStream = siddhiAppRuntime.getInputHandler("FooStream");

        siddhiAppRuntime.start();
        fooStream.send(new Object[]{"Request 1"});
        Thread.sleep(1000);
        siddhiManager.persist();
        Thread.sleep(100);
        siddhiAppRuntime.shutdown();
        Thread.sleep(100);

        siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition);
        fooStream = siddhiAppRuntime.getInputHandler("FooStream");
        siddhiAppRuntime.start();
        siddhiManager.restoreLastState();

        fooStream.send(new Object[]{"Request 2"});
        Thread.sleep(1000);
        siddhiAppRuntime.shutdown();

        Assert.assertTrue(((TestAppender) logger.getAppenders().
                get("TestAppender")).getMessages().contains("Server consume hit with payload = [Request 1] " +
                "and Headers = {{}}"));
        Assert.assertTrue(((TestAppender) logger.getAppenders().
                get("TestAppender")).getMessages().contains("Server consume hit with payload = [Request 2] " +
                "and Headers = {{}}"));
        logger.removeAppender(appender);
    }
}
