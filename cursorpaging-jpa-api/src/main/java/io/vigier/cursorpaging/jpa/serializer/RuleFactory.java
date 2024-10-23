package io.vigier.cursorpaging.jpa.serializer;

import io.vigier.cursorpaging.jpa.FilterRule;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface RuleFactory extends Function<Map<String, List<String>>, FilterRule> {

}
