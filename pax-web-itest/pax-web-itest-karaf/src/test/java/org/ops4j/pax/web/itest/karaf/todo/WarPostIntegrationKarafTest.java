/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.itest.karaf.todo;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.karaf.AbstractKarafTestBase;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WarPostIntegrationKarafTest extends AbstractKarafTestBase {

//	private static final Logger LOG = LoggerFactory
//			.getLogger(WarPostIntegrationKarafTest.class);
//
//	private Bundle installWarBundle;
//
//	@Configuration
//	public Option[] configurationDetailed() {
//		return combine(jettyConfig(), new VMOption("-DMyFacesVersion="
//				+ getMyFacesVersion()));
//
//	}
//
//	@Before
//	public void setUp() throws BundleException, InterruptedException {
//		LOG.info("Setting up test");
//
//		initWebListener();
//
//		String bundlePath = "mvn:org.ops4j.pax.web.samples/war-extended-post/"
//				+ VersionUtil.getProjectVersion() + "/war";
//		installWarBundle = bundleContext.installBundle(bundlePath);
//		installWarBundle.start();
//
//		waitForWebListener();
//	}
//
//
//	@After
//	public void tearDown() throws BundleException {
//		if (installWarBundle != null) {
//			installWarBundle.stop();
//			installWarBundle.uninstall();
//		}
//	}
//
//	@Test
//	public void testWC() throws Exception {
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.doGETandExecuteTest("http://127.0.0.1:8181/posttest/index.html");
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'POST data size is: 3000000'",
//						resp -> resp.contains("POST data size is: 3000000"))
//				.doPOST("http://127.0.0.1:8181/posttest/upload-check")
//				.addParameter("data", createData())
//				.executeTest();
//
////		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
////		nameValuePairs
////				.add(new BasicNameValuePair("data", createData()));
////		createTestClientForKaraf()
////				.doGETandExecuteTest("http://127.0.0.1:8181/posttest/index.html");
////		testClient.testWebPath("http://127.0.0.1:8181/posttest/index.html", 200);
////		testClient.testPost("http://127.0.0.1:8181/posttest/upload-check", nameValuePairs, "POST data size is: 3000000", 200);
//
//	}
//
//	private String createData() {
//		StringBuffer buff = new StringBuffer();
//
//		int i = 0;
//		while (i < 3000000) {
//			buff.append("A");
//			i++;
//		}
//
//		return buff.toString();
//	}

}