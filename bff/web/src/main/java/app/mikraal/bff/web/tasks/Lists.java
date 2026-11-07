package app.mikraal.bff.web.tasks;

import java.util.List;
import java.util.function.Function;

final class Lists {

    private Lists() {
    }

    /** Maps each element; a missing list stays missing. */
    static <A, B> List<B> map(List<A> list, Function<A, B> mapper) {
        return list == null ? null : list.stream().map(mapper).toList();
    }
}
