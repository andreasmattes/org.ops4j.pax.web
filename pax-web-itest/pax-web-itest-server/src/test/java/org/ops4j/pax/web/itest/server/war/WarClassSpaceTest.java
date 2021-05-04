/*
 * Copyright 2021 OPS4J.
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
 */
package org.ops4j.pax.web.itest.server.war;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import javax.servlet.ServletContainerInitializer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.itest.server.MultiContainerTestSupport;
import org.ops4j.pax.web.itest.server.support.war.cb1.scis.SCIFromContainerBundle1;
import org.ops4j.pax.web.itest.server.support.war.cb2.scis.SCIFromContainerBundle2;
import org.ops4j.pax.web.itest.server.support.war.cb3.scis.SCIFromContainerBundle3;
import org.ops4j.pax.web.itest.server.support.war.cf1.scis.SCIFromContainerFragment1;
import org.ops4j.pax.web.itest.server.support.war.cf2.scis.SCIFromContainerFragment2;
import org.ops4j.pax.web.itest.server.support.war.jar.scis.SCIFromJar;
import org.ops4j.pax.web.itest.server.support.war.scis.SCIFromTheFragment1;
import org.ops4j.pax.web.itest.server.support.war.scis.SCIFromTheFragment2;
import org.ops4j.pax.web.itest.server.support.war.scis.SCIFromTheWab1;
import org.ops4j.pax.web.itest.server.support.war.scis.SCIFromTheWab2;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

@RunWith(Parameterized.class)
public class WarClassSpaceTest extends MultiContainerTestSupport {

	@Override
	protected boolean enableWarExtender() {
		return true;
	}

	@Override
	protected boolean enableWhiteboardExtender() {
		return false;
	}

	@Test
	public void complexWab() throws Exception {
		// 1. the WAB bundle
		Bundle wab = mockBundle("the-wab-itself", "/wab");
		when(wab.getBundleId()).thenReturn(42L);
		configureBundleClassPath(wab, "src/test/resources/bundles/the-wab-itself", entries -> {
			entries.add("WEB-INF/classes");
			entries.add("WEB-INF/lib/the-wab-jar-8.0.0-SNAPSHOT.jar");
			entries.add("WEB-INF/lib/commons-io-2.8.0.jar");
			entries.add("WEB-INF/lib/commons-codec-1.13.jar");
		});
		String webXmlLocation = String.format("bundle://42.0:0%s",
				new File("src/test/resources/bundles/the-wab-itself/WEB-INF/web.xml").getCanonicalPath());
		when(wab.findEntries("WEB-INF", "web.xml", false))
				.thenReturn(Collections.enumeration(Collections.singletonList(new URL(webXmlLocation))));
		when(wab.loadClass(anyString()))
				.thenAnswer(i -> WarClassSpaceTest.class.getClassLoader().loadClass(i.getArgument(0, String.class)));

		// 2. the fragment bundle attached to the WAB
		Bundle wabFragment = mockBundle("the-wab-fragment", false);
		when(wabFragment.getBundleId()).thenReturn(43L);
		attachBundleFragment(wab, wabFragment);
		// root URL as an URL of the fragment
		when(wabFragment.getEntry("/")).thenReturn(new URL("bundle://43.0:0/"));
		// fragment is scanning "through" the host bundle
		String wabFragmentWebFragmentXmlLocation = String.format("bundle://42.0:0%s",
				new File("src/test/resources/bundles/the-wab-fragment/META-INF/web-fragment.xml").getCanonicalPath());
		when(wab.findEntries("/META-INF/", "web-fragment.xml", false))
				.thenReturn(Collections.enumeration(Collections.singletonList(new URL(wabFragmentWebFragmentXmlLocation))));

		// 3. container-bundle-1 - wired by Import-Package to the-wab-itself, providing another SCI and web-fragment.xml
		Bundle cb1 = mockBundle("container-bundle-1", false);
		when(cb1.getBundleId()).thenReturn(44L);
		when(cb1.getEntry("/")).thenReturn(new URL("bundle://44.0:0/"));
		// 4. container-fragment-1 - attached to container-bundle-1, providing another SCI and web-fragment.xml
		Bundle cf1 = mockBundle("container-fragment-1", false);
		when(cf1.getBundleId()).thenReturn(45L);
		when(cf1.getEntry("/")).thenReturn(new URL("bundle://45.0:0/"));
		attachBundleFragment(cb1, cf1);
		when(cf1.getEntry("/")).thenReturn(new URL("bundle://45.0:0/"));
		wireByPackage(wab, cb1, "org.ops4j.pax.web.itest.server.support.war.cb1.utils");

		// 5. container-bundle-2 - wired by Require-Bundle to the-wab-itself but through the-wab-fragment,
		//    providing another SCI and web-fragment.xml
		Bundle cb2 = mockBundle("container-bundle-2", false);
		when(cb2.getBundleId()).thenReturn(46L);
		when(cb2.getEntry("/")).thenReturn(new URL("bundle://46.0:0/"));
		// 6. container-fragment-2 - attached to container-bundle-2, providing another SCI and web-fragment.xml
		Bundle cf2 = mockBundle("container-fragment-2", false);
		when(cf2.getBundleId()).thenReturn(47L);
		when(cf2.getEntry("/")).thenReturn(new URL("bundle://47.0:0/"));
		attachBundleFragment(cb2, cf2);
		when(cf2.getEntry("/")).thenReturn(new URL("bundle://47.0:0/"));
		// at runtime, Require-Bundle from a fragment is actually visible through the host bundle itself
		wireByBundle(wab, cb2);

		// 7. container-bundle-3 - wired by Import-Package to container-bundle-1 (not wired directly to the-wab-itself),
		//    providing another SCI and web-fragment.xml
		Bundle cb3 = mockBundle("container-bundle-3", false);
		when(cb3.getBundleId()).thenReturn(48L);
		when(cb3.getEntry("/")).thenReturn(new URL("bundle://48.0:0/"));
		wireByPackage(cb1, cb3, "org.ops4j.pax.web.itest.server.support.war.cb3.utils");

		when(cb1.adapt(BundleWiring.class).findEntries("META-INF", "web-fragment.xml", 0))
				.thenReturn(Arrays.asList(
						new URL(String.format("bundle://44.0:0%s",
								new File("src/test/resources/bundles/container-bundle-1/META-INF/web-fragment.xml")
										.getCanonicalPath())),
						new URL(String.format("bundle://45.0:0%s",
								new File("src/test/resources/bundles/container-fragment-1/META-INF/web-fragment.xml")
										.getCanonicalPath()))
				));
		when(cb2.adapt(BundleWiring.class).findEntries("META-INF", "web-fragment.xml", 0))
				.thenReturn(Arrays.asList(
						new URL(String.format("bundle://46.0:0%s",
								new File("src/test/resources/bundles/container-bundle-2/META-INF/web-fragment.xml")
										.getCanonicalPath())),
						new URL(String.format("bundle://47.0:0%s",
								new File("src/test/resources/bundles/container-fragment-2/META-INF/web-fragment.xml")
										.getCanonicalPath()))
				));
		when(cb3.adapt(BundleWiring.class).findEntries("META-INF", "web-fragment.xml", 0))
				.thenReturn(Collections.singletonList(
						new URL(String.format("bundle://48.0:0%s",
								new File("src/test/resources/bundles/container-bundle-3/META-INF/web-fragment.xml")
										.getCanonicalPath()))
				));

		when(wab.getBundleContext().getBundle(44L)).thenReturn(cb1);
		when(wab.getBundleContext().getBundle(45L)).thenReturn(cf1);
		when(wab.getBundleContext().getBundle(46L)).thenReturn(cb2);
		when(wab.getBundleContext().getBundle(47L)).thenReturn(cf2);
		when(wab.getBundleContext().getBundle(48L)).thenReturn(cb3);

		File wabServices = new File("src/test/resources/bundles/the-wab-itself/WEB-INF/classes/META-INF/services/");
		File wabFragmentServices = new File("src/test/resources/bundles/the-wab-fragment/META-INF/services/");
		when(wab.findEntries(wabServices.getCanonicalPath() + "/",
				ServletContainerInitializer.class.getName(), false)).thenReturn(
				Collections.enumeration(Collections.singletonList(
						new File(wabServices, ServletContainerInitializer.class.getName()).toURI().toURL()))
		);
		when(wab.findEntries("/META-INF/services/",
				ServletContainerInitializer.class.getName(), false)).thenReturn(
				Collections.enumeration(Collections.singletonList(
						new File(wabFragmentServices, ServletContainerInitializer.class.getName()).toURI().toURL()))
		);
		File cb1Services = new File("src/test/resources/bundles/container-bundle-1/META-INF/services/");
		File cf1Services = new File("src/test/resources/bundles/container-fragment-1/META-INF/services/");
		when(cb1.findEntries("/META-INF/services/",
				ServletContainerInitializer.class.getName(), false)).thenReturn(
				Collections.enumeration(Arrays.asList(
						new File(cb1Services, ServletContainerInitializer.class.getName()).toURI().toURL(),
						new File(cf1Services, ServletContainerInitializer.class.getName()).toURI().toURL()
				))
		);
		File cb2Services = new File("src/test/resources/bundles/container-bundle-2/META-INF/services/");
		File cf2Services = new File("src/test/resources/bundles/container-fragment-2/META-INF/services/");
		when(cb2.findEntries("/META-INF/services/",
				ServletContainerInitializer.class.getName(), false)).thenReturn(
				Collections.enumeration(Arrays.asList(
						new File(cb2Services, ServletContainerInitializer.class.getName()).toURI().toURL(),
						new File(cf2Services, ServletContainerInitializer.class.getName()).toURI().toURL()
				))
		);
		File cb3Services = new File("src/test/resources/bundles/container-bundle-3/META-INF/services/");
		when(cb3.findEntries("/META-INF/services/",
				ServletContainerInitializer.class.getName(), false)).thenReturn(
				Collections.enumeration(Collections.singletonList(
						new File(cb3Services, ServletContainerInitializer.class.getName()).toURI().toURL()))
		);

		doReturn(SCIFromTheWab1.class)
				.when(wab).loadClass("org.ops4j.pax.web.itest.server.support.war.scis.SCIFromTheWab1");
		doReturn(SCIFromTheWab2.class)
				.when(wab).loadClass("org.ops4j.pax.web.itest.server.support.war.scis.SCIFromTheWab2");
		doReturn(SCIFromTheFragment1.class)
				.when(wab).loadClass("org.ops4j.pax.web.itest.server.support.war.scis.SCIFromTheFragment1");
		doReturn(SCIFromTheFragment2.class)
				.when(wab).loadClass("org.ops4j.pax.web.itest.server.support.war.scis.SCIFromTheFragment2");
		doReturn(SCIFromJar.class)
				.when(wab).loadClass("org.ops4j.pax.web.itest.server.support.war.jar.scis.SCIFromJar");
		doReturn(SCIFromContainerBundle1.class)
				.when(cb1).loadClass("org.ops4j.pax.web.itest.server.support.war.cb1.scis.SCIFromContainerBundle1");
		doReturn(SCIFromContainerFragment1.class)
				.when(cb1).loadClass("org.ops4j.pax.web.itest.server.support.war.cf1.scis.SCIFromContainerFragment1");
		doReturn(SCIFromContainerBundle2.class)
				.when(cb2).loadClass("org.ops4j.pax.web.itest.server.support.war.cb2.scis.SCIFromContainerBundle2");
		doReturn(SCIFromContainerFragment2.class)
				.when(cb2).loadClass("org.ops4j.pax.web.itest.server.support.war.cf2.scis.SCIFromContainerFragment2");
		doReturn(SCIFromContainerBundle3.class)
				.when(cb3).loadClass("org.ops4j.pax.web.itest.server.support.war.cb3.scis.SCIFromContainerBundle3");

		// /data/sources/github.com/ops4j/org.ops4j.pax.web/pax-web-itest/pax-web-itest-server/src/test/resources/bundles/the-wab-itself/WEB-INF/classes/
		File wabClasses = new File("src/test/resources/bundles/the-wab-itself/WEB-INF/classes/");
		when(wab.findEntries(wabClasses.getCanonicalPath() + "/", "*.class", true)).thenReturn(
				Collections.enumeration(Arrays.asList(
						getClass().getClassLoader().getResource("org/ops4j/pax/web/itest/server/support/war/Cb1IFace3Impl.class"),
						getClass().getClassLoader().getResource("org/ops4j/pax/web/itest/server/support/war/SimplestServlet.class")
				))
		);
		// classes for WAB's fragment - accessible using WAB bundle itself, so it returns the above classes as well
		when(wab.findEntries("/", "*.class", true)).thenReturn(
				Collections.enumeration(Arrays.asList(
						getClass().getClassLoader().getResource("org/ops4j/pax/web/itest/server/support/war/Cb1IFace3Impl.class"),
						getClass().getClassLoader().getResource("org/ops4j/pax/web/itest/server/support/war/SimplestServlet.class"),
						getClass().getClassLoader().getResource("org/ops4j/pax/web/itest/server/support/war/fragment/AnnotatedServlet1.class"),
						getClass().getClassLoader().getResource("org/ops4j/pax/web/itest/server/support/war/fragment/AnnotatedServlet2.class"),
						getClass().getClassLoader().getResource("org/ops4j/pax/web/itest/server/support/war/fragment/AnnotatedServlet3.class")
				))
		);
		when(cb1.findEntries("/", "*.class", true)).thenReturn(
				Collections.enumeration(Collections.singletonList(
						getClass().getClassLoader().getResource("org/ops4j/pax/web/itest/server/support/war/cb1/utils/Cb1IFace3.class")
				))
		);
		when(cb2.findEntries("/", "*.class", true)).thenReturn(Collections.enumeration(Collections.emptyList()));
		when(cb3.findEntries("/", "*.class", true)).thenReturn(
				Collections.enumeration(Collections.singletonList(
						getClass().getClassLoader().getResource("org/ops4j/pax/web/itest/server/support/war/cb3/utils/IFace3.class")
				))
		);

		// static resources
		when(wab.getResource("hello.txt")).thenReturn(new File("src/test/resources/bundles/the-wab-itself/hello.txt").toURI().toURL());
		when(wab.getResource("hello-fragment.txt")).thenReturn(new File("src/test/resources/bundles/the-wab-fragment/hello-fragment.txt").toURI().toURL());
		// for proper 302 redirect - Undertow doesn't handle such redirect when accessing root of the context.
		when(wab.getResource("/")).thenReturn(new File("src/test/resources/bundles/the-wab-itself/").toURI().toURL());

		installWab(wab);

		// there should be a /wab context that's (by default) redirecting to /wab/
		assertThat(httpGET(port, "/wab"), startsWith("HTTP/1.1 302"));

		// servlet from web.xml
		assertThat(httpGET(port, "/wab/servlet"), endsWith("Hello"));
		// servlet from a an SCI of container-bundle-3
		assertThat(httpGET(port, "/wab/dynamic1"), endsWith("Hello World!"));
		// annotated servlet from WAB fragment
		assertThat(httpGET(port, "/wab/as1/xyz"), endsWith("Hello /xyz!"));
		// resource from the WAB
		assertThat(httpGET(port, "/wab/hello.txt"),
				containsString("This is just a static resource in the root directory of the WAB."));
		// resource from the WAB's fragment
		assertThat(httpGET(port, "/wab/hello-fragment.txt"),
				containsString("This is just a static resource in the root directory of the WAB's fragment."));

		uninstallWab(wab);

		assertThat(httpGET(port, "/wab/servlet"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/wab/dynamic1"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/wab/as1/xyz"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/wab/hello.txt"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/wab/hello-fragment.txt"), startsWith("HTTP/1.1 404"));

		// there should be no /wab context at all, so no redirect, just 404
		assertThat(httpGET(port, "/wab"), startsWith("HTTP/1.1 404"));

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(wab);

		assertTrue(serverModelInternals.isClean(wab));
		assertTrue(serviceModelInternals.isEmpty());
	}

}