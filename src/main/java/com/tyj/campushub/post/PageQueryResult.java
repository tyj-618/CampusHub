package com.tyj.campushub.post;

import java.util.List;

public record PageQueryResult<T>(
        long total,
        List<T> records
) {
}
