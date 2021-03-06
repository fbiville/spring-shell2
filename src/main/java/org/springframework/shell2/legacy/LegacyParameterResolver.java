package org.springframework.shell2.legacy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.shell.core.Converter;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell2.ParameterResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Created by ericbottard on 09/12/15.
 */
@Component
public class LegacyParameterResolver implements ParameterResolver {

	public static final Object UNSPECIFIED = new Object();

	@Autowired(required = false)
	private Collection<Converter<?>> converters = new ArrayList<>();

	@Override
	public boolean supports(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CliOption.class);
	}

	@Override
	public Object resolve(MethodParameter methodParameter, List<String> words) {
		CliOption cliOption = methodParameter.getParameterAnnotation(CliOption.class);
		Optional<Converter<?>> converter = converters.stream()
				.filter(c -> c.supports(methodParameter.getParameterType(), cliOption.optionContext()))
				.findFirst();

		Map<String, String> values = parseOptions(words);
		Map<String, Object> seenValues = convertValues(values, methodParameter, converter);
		switch (seenValues.size()) {
			case 0:
				if (!cliOption.mandatory()) {
					String value = cliOption.unspecifiedDefaultValue();
					return converter
							.orElseThrow(noConverterFound(cliOption.key()[0], value, methodParameter.getParameterType()))
							.convertFromText(value, methodParameter.getParameterType(), cliOption.optionContext());
				}
				else {
					throw new IllegalArgumentException("Could not find parameter values for " + prettifyKeys(Arrays.asList(cliOption.key())) + " in " + words);
				}
			case 1:
				return seenValues.values().iterator().next();
			default:
				throw new RuntimeException("Option has been set multiple times via " + prettifyKeys(seenValues.keySet()));
		}
	}

	private Map<String, String> parseOptions(List<String> words) {
		Map<String, String> values = new HashMap<>();
		for (int i = 0; i < words.size(); i++) {
			String word = words.get(i);
			if (word.startsWith("--")) {
				String key = word.substring("--".length());
				// If next word doesn't exist or starts with '--', this is an unary option. Store null
				String value = i < words.size() - 1 && !words.get(i+1).startsWith("--") ? words.get(++i) : null;
				Assert.isTrue(!values.containsKey(key), String.format("Option --%s has already been set", key));
				values.put(key, value);
			} // Must be the 'anonymous' option
			else {
				Assert.isTrue(!values.containsKey(""), "Anonymous option has already been set");
				values.put("", word);
			}
		}
		return values;
	}

	private Map<String, Object> convertValues(Map<String, String> values, MethodParameter methodParameter, Optional<Converter<?>> converter) {
		Map<String, Object> seenValues = new HashMap<>();
		CliOption option = methodParameter.getParameterAnnotation(CliOption.class);
		for (String key : option.key()) {
			if (values.containsKey(key)) {
				String value = values.get(key);
				if (value == null && !"__NULL__".equals(option.specifiedDefaultValue())) {
					value = option.specifiedDefaultValue();
				}
				Class<?> parameterType = methodParameter.getParameterType();
				seenValues.put(key, converter
						.orElseThrow(noConverterFound(key, value, parameterType))
						.convertFromText(value, parameterType, option.optionContext()));
			}
		}
		return seenValues;
	}

	/**
	 * Return the list of possible keys for an option, suitable for displaying in an error message.
	 */
	private String prettifyKeys(Collection<String> keys) {
		return keys.stream().map(s -> "".equals(s) ? "<anonymous>" : "--" + s).collect(Collectors.joining(", ", "[", "]"));
	}

	private Supplier<IllegalStateException> noConverterFound(String key, String value, Class<?> parameterType) {
		return () -> new IllegalStateException("No converter found for --" + key + " from '" + value + "' to type " + parameterType);
	}

}
