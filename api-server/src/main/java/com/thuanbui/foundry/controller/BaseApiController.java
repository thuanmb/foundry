package com.thuanbui.foundry.controller;

/**
 * Base class for all API controllers.
 * Request ID (rid) is automatically set by {@code RequestIdFilter}
 * and included in all MDC-aware log output via {@code [%X{rid}]}.
 */
public abstract class BaseApiController {
}
