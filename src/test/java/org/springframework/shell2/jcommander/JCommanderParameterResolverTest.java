package org.springframework.shell2.jcommander;

import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by ericbottard on 15/12/15.
 */
public class JCommanderParameterResolverTest {

	private static final Method COMMAND_METHOD = ReflectionUtils.findMethod(MyLordCommands.class, "genesis", FieldCollins.class);

	private JCommanderParameterResolver resolver = new JCommanderParameterResolver();

	@Test
	public void testSupportsJCommanderPojos() throws Exception {
		assertThat(resolver.supports(new MethodParameter(COMMAND_METHOD, 0))).isEqualTo(true);
	}

	@Test
	public void testDoesNotSupportsNonJCommanderPojos() throws Exception {
		Method method = ReflectionUtils.findMethod(MyLordCommands.class, "apocalypse", String.class);

		assertThat(resolver.supports(new MethodParameter(method, 0))).isFalse();
	}

	@Test
	public void testPojoValuesAreCorrectlySet() {
		MethodParameter methodParameter = new MethodParameter(COMMAND_METHOD, 0);

		FieldCollins resolved = (FieldCollins) resolver.resolve(methodParameter, asList("--name foo -level 2 something-else yet-something-else".split(" ")));

		assertThat(resolved.getName()).isEqualTo("foo");
		assertThat(resolved.getLevel()).isEqualTo(2);
		assertThat(resolved.getRest()).containsOnlyOnce("something-else", "yet-something-else");
	}
}
