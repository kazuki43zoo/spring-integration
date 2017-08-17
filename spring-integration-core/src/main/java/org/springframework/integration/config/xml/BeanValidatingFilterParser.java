/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.integration.config.xml;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.FilterFactoryBean;
import org.springframework.integration.selector.BeanValidatingMessageSelector;
import org.springframework.util.StringUtils;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.w3c.dom.Element;

/**
 * Parser for {@code <int:bean-validating-filter />}.
 *
 * @author Kazuki Shimizu
 * @since 5.0
 */
public class BeanValidatingFilterParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder selectorBuilder = BeanDefinitionBuilder.genericBeanDefinition(BeanValidatingMessageSelector.class);
		String validatorBeanName = element.getAttribute("validator");
		if (StringUtils.hasText(validatorBeanName)) {
			selectorBuilder.addConstructorArgReference(validatorBeanName);
		} else {
			selectorBuilder.addConstructorArgValue(new LocalValidatorFactoryBean());
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(selectorBuilder, element, "throw-exception-on-rejection");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(selectorBuilder, element, "validation-hints-resolver");

		BeanDefinitionBuilder filterBuilder = BeanDefinitionBuilder.genericBeanDefinition(FilterFactoryBean.class);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(filterBuilder, element, "discard-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(filterBuilder, element, "throw-exception-on-rejection");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(filterBuilder, element, "send-timeout");
		filterBuilder.addPropertyValue("targetObject", selectorBuilder.getBeanDefinition());

		return filterBuilder;
	}

}
