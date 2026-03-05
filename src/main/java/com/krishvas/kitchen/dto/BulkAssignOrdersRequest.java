package com.krishvas.kitchen.dto;

import java.util.List;

public record BulkAssignOrdersRequest(
    List<String> orderIds
) {}
