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

package org.springframework.integration.selector;

import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;

/**
 * A {@link MessageSelector} for validating payload of {@link Message} using Spring's {@link Validator}.
 *
 * @author Kazuki Shimizu
 * @since 5.0
 */
public class BeanValidatingMessageSelector implements MessageSelector {

	private static final Object[] DEFAULT_VALIDATION_HINTS = new Object[0];

	private final Validation validation;
	private ValidationHintsResolver validationHintsResolver = message -> DEFAULT_VALIDATION_HINTS;
	private boolean throwExceptionOnRejection;

	/**
	 * Constructor.
	 *
	 * @param validator A Spring's {@link Validator}
	 */
	public BeanValidatingMessageSelector(Validator validator) {
		Assert.notNull(validator, "'validator' must not be null");
		if (validator instanceof SmartValidator) {
			this.validation = (message, bindingResult) ->
				((SmartValidator) validator).validate(message.getPayload(), bindingResult, validationHintsResolver.resolve(message));
		} else {
			this.validation = (message, bindingResult) -> validator.validate(message.getPayload(), bindingResult);
		}
	}

	/**
	 * Specify a custom {@link ValidationHintsResolver}.
	 * By default, {@link ValidationHintsResolver} that return empty array is used.
	 *
	 * @param validationHintsResolver A custom {@link ValidationHintsResolver}
	 */
	public void setValidationHintsExtractor(ValidationHintsResolver validationHintsResolver) {
		Assert.notNull(validationHintsResolver, "'validationHintsResolver' must not be null");
		this.validationHintsResolver = validationHintsResolver;
	}

	/**
	 * Specify whether this filter should throw a
	 * {@link MessageRejectedException} when its selector does not accept a
	 * Message. The default value is <code>false</code> meaning that rejected
	 * Messages will be quietly dropped or sent to the discard channel if
	 * available. Typically this value would not be <code>true</code> when
	 * a discard channel is provided, but if so, it will still apply
	 * (in such a case, the Message will be sent to the discard channel,
	 * and <em>then</em> the exception will be thrown).
	 *
	 * @param throwExceptionOnRejection <code>true</code> if an exception should be thrown.
	 * @see org.springframework.integration.filter.MessageFilter#setDiscardChannel(MessageChannel)
	 */
	public void setThrowExceptionOnRejection(boolean throwExceptionOnRejection) {
		this.throwExceptionOnRejection = throwExceptionOnRejection;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean accept(Message<?> message) {
		String objectName = StringUtils.uncapitalize(message.getPayload().getClass().getSimpleName());
		BindingResult bindingResult = new BeanPropertyBindingResult(message.getPayload(), objectName);

		validation.execute(message, bindingResult);

		if (bindingResult.hasErrors()) {
			if (this.throwExceptionOnRejection) {
				throw new MessageRejectedException(message,
					"Message was rejected due to Bean Validation errors", new BindException(bindingResult));
			}
			return false;
		}
		return true;
	}

	@FunctionalInterface
	private interface Validation {
		void execute(Message<?> message, BindingResult bindingResult);
	}

	/**
	 * The interface that resolve validation hint objects.
	 */
	@FunctionalInterface
	public interface ValidationHintsResolver {
		/**
		 * Extract hint objects to be passed to the validation engine via Spring's {@link SmartValidator}.
		 */
		Object[] resolve(Message<?> message);
	}

}
