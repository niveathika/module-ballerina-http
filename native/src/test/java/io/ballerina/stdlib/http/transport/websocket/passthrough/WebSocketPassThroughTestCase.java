/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.stdlib.http.transport.websocket.passthrough;

import io.ballerina.stdlib.http.transport.contract.ServerConnector;
import io.ballerina.stdlib.http.transport.contract.ServerConnectorFuture;
import io.ballerina.stdlib.http.transport.contract.config.ListenerConfiguration;
import io.ballerina.stdlib.http.transport.contractimpl.DefaultHttpWsConnectorFactory;
import io.ballerina.stdlib.http.transport.util.TestUtil;
import io.ballerina.stdlib.http.transport.util.client.websocket.WebSocketTestClient;
import io.ballerina.stdlib.http.transport.util.server.websocket.WebSocketRemoteServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import static io.ballerina.stdlib.http.transport.util.TestUtil.WEBSOCKET_TEST_IDLE_TIMEOUT;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Test cases for WebSocket pass-through scenarios.
 */
public class WebSocketPassThroughTestCase {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketPassThroughTestCase.class);

    private DefaultHttpWsConnectorFactory httpConnectorFactory;
    private WebSocketRemoteServer remoteServer;
    private ServerConnector serverConnector;

    @BeforeClass
    public void setup() throws InterruptedException {
        remoteServer = new WebSocketRemoteServer(TestUtil.WEBSOCKET_REMOTE_SERVER_PORT);
        remoteServer.run();
        ListenerConfiguration listenerConfiguration = new ListenerConfiguration();
        listenerConfiguration.setHost("localhost");
        listenerConfiguration.setPort(TestUtil.SERVER_CONNECTOR_PORT);
        httpConnectorFactory = new DefaultHttpWsConnectorFactory();
        serverConnector = httpConnectorFactory.createServerConnector(TestUtil.getDefaultServerBootstrapConfig(),
                                                                     listenerConfiguration);
        ServerConnectorFuture connectorFuture = serverConnector.start();
        connectorFuture.setWebSocketConnectorListener(new WebSocketPassThroughServerConnectorListener());
        connectorFuture.sync();
    }

    // TODO disabled due to https://github.com/ballerina-platform/module-ballerina-http/issues/78
    @Test(enabled = false)
    public void testTextPassThrough() throws InterruptedException, URISyntaxException {
        CountDownLatch latch = new CountDownLatch(1);
        WebSocketTestClient webSocketClient = new WebSocketTestClient();
        webSocketClient.handshake();
        webSocketClient.setCountDownLatch(latch);
        String text = "hello-pass-through";
        webSocketClient.sendText(text);
        latch.await(WEBSOCKET_TEST_IDLE_TIMEOUT, SECONDS);

        Assert.assertEquals(webSocketClient.getTextReceived(), text);

        webSocketClient.sendCloseFrame(1001, "Going away");
    }

    // TODO disabled due to https://github.com/ballerina-platform/module-ballerina-http/issues/78
    @Test(enabled = false)
    public void testBinaryPassThrough() throws InterruptedException, URISyntaxException {
        CountDownLatch latch = new CountDownLatch(1);
        WebSocketTestClient webSocketClient = new WebSocketTestClient();
        webSocketClient.handshake();
        webSocketClient.setCountDownLatch(latch);
        ByteBuffer sentBuffer = ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5});
        webSocketClient.sendBinary(sentBuffer);
        latch.await(WEBSOCKET_TEST_IDLE_TIMEOUT, SECONDS);
        ByteBuffer bufferReceived = webSocketClient.getBufferReceived();
        Assert.assertNotNull(bufferReceived);
        Assert.assertEquals(bufferReceived, sentBuffer);

        webSocketClient.sendCloseFrame(1001, "Going away");
    }

    @AfterClass
    public void cleaUp() throws InterruptedException {
        serverConnector.stop();
        remoteServer.stop();
        httpConnectorFactory.shutdown();
    }
}
