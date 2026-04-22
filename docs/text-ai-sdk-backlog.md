# Text AI SDK Backlog

## Priority Order

### P1. OpenAI-compatible transport customization

**Why first**

This has the biggest payoff for future provider compatibility while still keeping the package pure-text.

**Scope**

- custom endpoint path override
- extra query parameters
- extra request headers

**Value**

- easier integration with OpenAI-compatible gateways
- better support for provider-specific routing details
- cleaner migration path toward broader OpenAI protocol compatibility

### P2. Stream lifecycle listener

**Scope**

- keep `DeltaCallback`
- add an optional richer listener with lifecycle hooks such as:
  - start
  - delta
  - complete
  - error
  - usage

**Value**

- better GUI integration
- cleaner stream instrumentation
- easier future support for richer OpenAI-compatible stream metadata

### P3. Response metadata enrichment

**Scope**

- expose response ID
- expose model name
- expose raw headers or selected provider metadata

**Value**

- better debugging
- easier auditing
- clearer interoperability with different providers

### P4. Retry and resilience policy

**Scope**

- configurable retries for transient failures
- retry-safe boundaries
- explicit handling for 429/5xx classes

**Value**

- stronger production readiness
- fewer temporary failures leaking into callers

### P5. Compatibility example suite

**Scope**

- provider-focused examples
- gateway-focused examples
- more documented request/response variations

**Value**

- easier adoption
- clearer OpenAI-compatible story
- lower integration cost for users
