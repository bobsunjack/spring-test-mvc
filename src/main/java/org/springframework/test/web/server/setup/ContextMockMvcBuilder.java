/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.server.setup;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockRequestDispatcher;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.server.MockMvc;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

/**
 * A {@link MockMvc} builder that helps to configure and initialize a WebApplicationContext
 * context before looking up Spring MVC components in it.
 * 
 * <p>The WebApplicationContext can be configured with the path to the web application root 
 * directory (classpath or file system relative), specific profiles can be activated, or 
 * {@link ApplicationContextInitializer}s applied.
 * 
 * @author Rossen Stoyanchev
 */
public class ContextMockMvcBuilder extends AbstractContextMockMvcBuilder {

	private final ConfigurableWebApplicationContext applicationContext;
	
	private String webResourceBasePath = "";

	private ResourceLoader webResourceLoader = new FileSystemResourceLoader();
	
	/**
	 * Create an instance with the given {@link ConfigurableWebApplicationContext}.
	 * The context will be refreshed when {@link #initWebApplicationContext} is called.
	 */
	public ContextMockMvcBuilder(ConfigurableWebApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	protected ServletContext initServletContext() {
		return new MockServletContext(this.webResourceBasePath, this.webResourceLoader) {
			// Required for DefaultServletHttpRequestHandler...
			public RequestDispatcher getNamedDispatcher(String path) {
				return (path.equals("default")) ? new MockRequestDispatcher(path) : super.getNamedDispatcher(path); 
			}			
		};
	}

	@Override
	protected WebApplicationContext initWebApplicationContext(ServletContext servletContext) {
		this.applicationContext.setServletContext(servletContext);
		this.applicationContext.refresh();
		return this.applicationContext;
	}
	
	/**
	 * Specify the location of the web application root directory. 
	 * <p>If {@code isClasspathRelative} is "false" the directory is interpreted either as being 
	 * relative to the JVM working directory (e.g. "src/main/webapp") or as a fully qualified 
	 * file system path (e.g. "file:///home/user/webapp"). 
	 * <p>Otherwise if {@code isClasspathRelative} is "true" the directory should be relative 
	 * to the classpath (e.g. "org/examples/myapp/config"). 
	 *  
	 * @param warRootDir the Web application root directory (should not end with a slash)
	 */
	public ContextMockMvcBuilder configureWarRootDir(String warRootDir, boolean isClasspathRelative) {
		this.webResourceBasePath = warRootDir;
		this.webResourceLoader = isClasspathRelative ? new DefaultResourceLoader() : new FileSystemResourceLoader();
		return this;
	}
	
	/**
	 * Activate the given profiles before the application context is "refreshed".
	 */
	public ContextMockMvcBuilder activateProfiles(String...profiles) {
		this.applicationContext.getEnvironment().setActiveProfiles(profiles);
		return this;
	}
	
	/**
	 * Apply the given {@link ApplicationContextInitializer}s before the application context is "refreshed".
	 */
	@SuppressWarnings("unchecked")
	public <T extends ConfigurableWebApplicationContext> 
			ContextMockMvcBuilder applyInitializers(ApplicationContextInitializer<T>... initializers) {
		
		for (ApplicationContextInitializer<T> initializer : initializers) {
			initializer.initialize((T) this.applicationContext);
		}
		return this;
	}

}
