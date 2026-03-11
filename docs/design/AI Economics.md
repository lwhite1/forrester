# AI Economics — Shrewd Platform

## 1. The Cost Structure

The AI integration is the dominant operating cost of the Shrewd platform. The simulation engine, file I/O, canvas rendering, and model persistence are essentially free to operate. Every dollar of marginal cost comes from LLM API calls.

### 1.1 Per-Call Token Budget

Each LLM call assembles a prompt from five components:

| Component | Token Range | Notes |
|---|---|---|
| Model context | 3,000–5,000 | Full `ModelDefinition` serialized — all element names, types, equations, units, connections, maturity signals. Non-negotiable: the LLM must see the complete model. An 80-stock model with equations sits at the high end. |
| Conversation history | 2,000–4,000 | Last 20 messages (user + LLM), windowed. Older messages summarized into a rolling "conversation so far" block. |
| Posture block | 500–1,000 | Dynamic system prompt selected by the maturity system. Varies by posture — AUTONOMIST is the longest, OBSERVER the shortest. |
| User message | 100–500 | Variable. Natural language tends longer than command input. |
| **Total input** | **~6,000–10,000** | Before the response. |

Output tokens (the LLM's response) add roughly 500–2,000 tokens per call depending on posture. INTERPRETER and CHALLENGER produce longer responses; OBSERVER produces very short ones.

### 1.2 Per-Session Cost Estimates

| Persona | Calls/Session | Avg Input Tokens | Estimated Session Cost | Notes |
|---|---|---|---|---|
| David (researcher) | 150–200 | 8,000–10,000 | $2–$8 | Heavy command palette use, long sessions, large models |
| Maya (student) | 50–100 | 6,000–8,000 | $0.75–$3 | Moderate sessions, smaller models, more conversational |
| Elena (decision-maker) | 30–50 | 7,000–9,000 | $1–$3 | AI-only mode, shorter sessions but heavier per-turn LLM work |

These estimates assume current Claude API pricing (circa 2025–2026). API pricing has dropped roughly 10x over the past two years and shows no sign of stopping, so these numbers will likely decline.

### 1.3 AI-Only Mode Amplifies Cost Per Turn

In AUTONOMIST posture, a single user turn can trigger multiple silent operations — five `add_stock` calls, six `set_equation` calls, `run_simulation`, interpretation — each potentially requiring a separate LLM call or a long multi-tool-use response. Scenario exploration ("I tested three scenarios") multiplies this further.

The per-session cost for AI-only mode is moderate (Elena's sessions are short), but the cost *per user interaction* is higher than canvas mode because:

- The LLM does more work per turn (conceptualize + formalize + simulate + interpret, all in one exchange)
- Natural language input is wordier than command palette or equation input
- Calibration rounds ("how many nurses?", "how long does ramp-up take?") are LLM-mediated rather than direct edits

### 1.4 Background Costs

Beyond user-facing calls, the system generates background LLM calls:

- **Conversation summarization:** Every 10 messages, a background call summarizes older messages into a rolling context block. Can use a cheaper/smaller model.
- **Posture evaluation:** The posture selection itself is deterministic (no LLM call), but posture transition hysteresis requires maintaining state across turns.

---

## 2. API Volume Pricing and Cost Reduction Mechanisms

There are no publicly listed volume discount tiers from major LLM providers. Both Anthropic and OpenAI offer negotiated enterprise pricing on a case-by-case basis — "contact sales" arrangements with no published schedule. However, there are several structural cost reduction mechanisms that are publicly available and can be combined.

### 2.1 Prompt Caching — Up to 90% Off Input Tokens

This is the single largest cost lever for Shrewd. Anthropic's prompt caching stores repeated prompt prefixes and charges cache reads at 1/10th the base input price. The write cost (first time the content enters cache) is 1.25x base price, but subsequent reads within the cache TTL (5 minutes default, 1 hour optional) are dramatically cheaper.

**Current pricing (per million tokens):**

| Model | Base Input | Cache Write (5min) | Cache Write (1hr) | Cache Hit | Savings on Hit |
|---|---|---|---|---|---|
| Opus 4.6 | $5.00 | $6.25 | $10.00 | $0.50 | 90% |
| Sonnet 4.6 | $3.00 | $3.75 | $6.00 | $0.30 | 90% |
| Haiku 4.5 | $1.00 | $1.25 | $2.00 | $0.10 | 90% |

**Impact on Shrewd:** The system prompt + posture block + tool-use schema are identical across turns. The model context changes slightly per turn but the bulk is repeated. For a typical 8,000-token input call, roughly 5,000–6,000 tokens are cacheable. With cache hits, the effective input cost drops from ~$0.024/call to ~$0.006/call on Sonnet — a 75% reduction on the input side.

The 5-minute TTL is sufficient for interactive sessions (turns usually happen within seconds to minutes). The 1-hour TTL (at 2x write cost) would cover breaks during a session but costs more on the initial write.

### 2.2 Batch API — 50% Off Input and Output

Asynchronous processing with 24-hour turnaround at half price:

| Model | Batch Input | Batch Output |
|---|---|---|
| Opus 4.6 | $2.50/MTok | $12.50/MTok |
| Sonnet 4.6 | $1.50/MTok | $7.50/MTok |
| Haiku 4.5 | $0.50/MTok | $2.50/MTok |

**Impact on Shrewd:** Not useful for interactive conversation (latency matters). Potentially useful for:
- **Background conversation summarization** — the rolling summary generated every 10 messages doesn't need to be instant. A 24-hour window is too long, but the batch discount signals that non-real-time workloads may get preferred pricing in future offerings.
- **AUTONOMIST multi-step model building** — if the user is willing to wait (e.g., "build me a model of nurse turnover and I'll come back"), the entire conceptualize-formalize-simulate-interpret chain could run as a batch job. This is a UX tradeoff: faster iteration vs. lower cost.

Currently batch processing and prompt caching discounts can be combined (stacked), which further reduces costs for batch workloads with repeated context.

### 2.3 Tiered Model Usage — 5x Price Spread

The price difference between model tiers is substantial:

| Model | Input/MTok | Output/MTok | Relative Cost |
|---|---|---|---|
| Haiku 4.5 | $1.00 | $5.00 | 1x (baseline) |
| Sonnet 4.6 | $3.00 | $15.00 | 3x |
| Opus 4.6 | $5.00 | $25.00 | 5x |

Not all LLM calls require the same capability. Routing by posture/call type:

| Call Type | Recommended Model | Per-Call Input Cost (8K tokens) | Notes |
|---|---|---|---|
| OBSERVER acknowledgments | Haiku 4.5 | $0.008 | Simple confirmation messages |
| Clarifying questions | Haiku 4.5 | $0.008 | Short, formulaic responses |
| Conversation summarization | Haiku 4.5 | $0.008 | Background task, quality tolerant |
| ELICITOR questions | Sonnet 4.6 | $0.024 | Needs domain reasoning |
| FORMALIZER equation help | Sonnet 4.6 | $0.024 | Needs math + domain knowledge |
| ANTICIPATOR predictions | Sonnet 4.6 | $0.024 | Needs model understanding |
| INTERPRETER analysis | Sonnet 4.6 or Opus | $0.024–$0.040 | Depends on complexity |
| CHALLENGER structural proposals | Opus 4.6 | $0.040 | Hardest reasoning task |
| AUTONOMIST model building | Opus 4.6 | $0.040 | Needs full capability |

**Estimated blended savings:** If a typical David session (200 calls) breaks down as 40% Haiku-eligible, 40% Sonnet, 20% Opus, the blended input cost is roughly $0.022/call vs. $0.040/call for all-Opus — a 45% reduction.

### 2.4 Combined Savings Estimate

Stacking prompt caching + model tiering on a David session (200 calls):

| Strategy | Input Cost/Call | Output Cost/Call | Total/Call | Session Cost (200 calls) |
|---|---|---|---|---|
| Baseline (all Opus, no caching) | $0.040 | $0.025 | $0.065 | $13.00 |
| + Prompt caching (75% input reduction) | $0.010 | $0.025 | $0.035 | $7.00 |
| + Model tiering (blended models) | $0.006 | $0.014 | $0.020 | $4.00 |
| + Both combined | $0.003 | $0.014 | $0.017 | $3.40 |

This brings the heaviest use case (David, 200 calls) from $13 down to roughly $3–4 per session. Maya's sessions (50–100 calls) would run $0.85–$1.70. Elena's AI-only sessions (30–50 calls) would run $0.50–$0.85.

### 2.5 Enterprise / Volume Discounts

Neither Anthropic nor OpenAI publishes volume discount schedules. Both offer negotiated enterprise pricing:

- **Anthropic:** "Volume discounts may be available for high-volume users. These are negotiated on a case-by-case basis." Contact sales@anthropic.com. Enterprise arrangements include custom rate limits, dedicated support, and custom terms.
- **OpenAI:** Enterprise plans include "invoicing and billing with volume discounts" and reserved capacity options. Also negotiated, not published.
- **AWS Bedrock / Google Vertex AI / Azure:** Claude is available through these platforms, which have their own enterprise pricing structures, committed-use discounts, and enterprise agreements. An institutional buyer already on AWS might get better effective rates through Bedrock than through Anthropic directly.

For a platform like Shrewd operating as an intermediary service (Option A), the negotiation leverage depends on volume. At 1,000 daily active users averaging 50 calls each (50,000 calls/day), the monthly token volume would be substantial enough to justify a conversation with Anthropic's enterprise sales team. The discount would likely be 10–30% off published rates based on typical enterprise SaaS API agreements, though this is speculative — actual terms depend on the specific negotiation.

### 2.6 Additional Cost Reduction Strategies

**Delta-based model context.** Instead of serializing the full `ModelDefinition` on every call, send only what changed since the last call. Most turns change 0–3 elements. A delta representation could reduce the model context from 3,000–5,000 tokens to 200–500 tokens for typical calls, with periodic full snapshots. This stacks with prompt caching — fewer tokens to cache-write means lower write costs.

**Tradeoff:** Increases implementation complexity and creates a failure mode if the LLM's cached model state drifts from reality. Requires careful state management.

**Aggressive conversation summarization.** The current design windows the last 20 messages. More aggressive summarization (last 10 messages, or summarize-on-every-turn) reduces conversation history tokens at the cost of the LLM losing conversational nuance.

**Output token management.** Output tokens are 3–5x more expensive than input tokens. Posture prompts can instruct the LLM to be concise (OBSERVER already does this). For INTERPRETER and CHALLENGER, where longer responses are valuable, this is a quality-vs-cost tradeoff. Setting `max_tokens` on the API call provides a hard cap.

---

## 3. Delivery Model Options

### 3.1 Option A: Intermediary Service (Relay to Cloud LLM)

Users connect to a Shrewd-operated API, which in turn calls Claude/GPT/etc. The platform owns the billing relationship with the LLM provider; users pay the platform.

**Advantages:**

- **Margin capture.** Mark up the API cost. A $3 session costs $3 in API fees; charge $10/month or per-session. The margin funds development.
- **Rate limiting and abuse control.** Throttle heavy users, cap sessions, implement quotas — impossible if users bring their own API key.
- **Prompt protection.** Posture prompts, system instructions, and tool-use definitions stay server-side. Users never see them. The posture system — the six behavioral modes, the maturity-to-posture mapping, the prompt engineering — is core IP. If users call the API directly with their own key, all of that ships in the client binary where it can be extracted.
- **Model routing.** Swap models silently per call type. Use a small model for OBSERVER acknowledgments and a large model for CHALLENGER reasoning. The tiered strategy becomes an operational decision, not something baked into the client.
- **Usage analytics.** Every call is visible: which postures are used, calls per session, where users drop off, which postures produce the best outcomes. This is essential for improving the product. Direct API keys give no visibility.
- **Background processing.** Conversation summarization runs on the service infrastructure at off-peak pricing or with a cheaper model.
- **Prompt iteration.** Update posture prompts server-side without shipping client updates. Given the spec expects 20+ prompt revisions per posture, this matters enormously during development and ongoing tuning.

**Disadvantages:**

- **Operational burden.** Running a service means uptime, infrastructure, authentication, billing systems, support. The product shifts from a desktop application to a service-dependent client.
- **Financial risk.** A runaway session (user leaves a loop running, abuse scenario) costs money even if the user doesn't pay more. Requires guardrails.
- **Added latency.** User to service to LLM provider and back. Adds 50–200ms per call depending on geography.
- **Privacy exposure.** The service sees users' model data and conversations. For healthcare data (Elena's hospital scenario), this creates compliance obligations — HIPAA, institutional data governance, potentially GDPR for European users.
- **Dependency.** Users can't work offline (no LLM access). The canvas-based modeling works without AI, but the conversation panel is dead without the service.

### 3.2 Option B: Self-Hosted LLM

Run an open-weight model (Llama, Mistral, DeepSeek, Qwen, etc.) on owned or leased GPU infrastructure.

**Advantages:**

- **Fixed cost, not per-token.** A GPU instance costs the same whether it handles 10 calls or 10,000. At scale, this is dramatically cheaper. A single A100 running a 70B-parameter model can handle roughly 50–100 concurrent requests. For 1,000 daily active users averaging 50 calls each, per-session cost drops well below $1.
- **No third-party dependency.** LLM providers can change pricing, impose rate limits, deprecate models, or change terms of service. Self-hosting eliminates vendor risk.
- **Data sovereignty.** Data never leaves the hosting environment. "Your nurse turnover data never leaves the hospital network" is a selling point no cloud API can match. Critical for institutional deployments in healthcare, government, and defense.
- **Fine-tuning.** Domain-specific fine-tuning on system dynamics knowledge — published models, SD textbook structures, archetype libraries, equation patterns — becomes feasible. The spec flags that ELICITOR quality depends on domain knowledge and that "there is no good mitigation short of domain-specific fine-tuning or retrieval augmentation." Self-hosting makes this practical.
- **No per-call metering.** Removes the incentive to limit LLM interactions. Users can experiment freely without cost anxiety. This matters for the student persona — institutional budget holders are wary of open-ended per-use charges.

**Disadvantages:**

- **Model quality gap.** The posture system — especially CHALLENGER and AUTONOMIST — requires strong reasoning, nuanced instruction-following, and domain knowledge. Current open-weight 70B models are meaningfully worse than Claude Opus or GPT-4 at the complex behavioral adherence the spec demands. The spec expects 20+ prompt revisions per posture to get reliable behavior from frontier models; weaker models may never reach acceptable quality for the hardest postures.
- **Tool use reliability.** The function-calling protocol (`add_stock`, `set_equation`, etc.) works reliably on Claude and GPT-4. Open-weight models have spottier tool-use adherence — they hallucinate parameters, call tools in wrong order, or ignore the schema. This directly breaks the diff-card workflow that is central to the user experience.
- **Infrastructure burden.** GPU instances are expensive to maintain ($1–$2/hour for cloud A100s), hard to scale elastically, and require ML operations expertise. This is a different kind of operational burden than running a relay service.
- **Upfront capital.** Cloud API costs scale linearly from zero users. Self-hosting has a cost floor — GPU instances run whether there are 5 users or 500.
- **Model evolution.** Frontier API models improve continuously (new releases, capability upgrades). Self-hosted models require manual updates, re-testing of all posture prompts, and potential fine-tuning rework with each model change.

### 3.3 Option C: Bring Your Own Key (BYOK)

Ship the client with the ability for users to plug in their own API key. The client calls the LLM provider directly.

**Advantages:**

- **Zero operating cost for the platform.** Users pay their own API bills. No service to run, no margin to manage, no financial risk.
- **Simplicity.** No backend infrastructure. The product is a desktop application, full stop.

**Disadvantages:**

- **Exposes all IP.** Posture prompts, system instructions, tool-use schemas — everything ships in the client binary. Competitors can extract and replicate the entire AI integration strategy.
- **No analytics.** No visibility into usage patterns, posture effectiveness, or failure modes.
- **No model routing.** Can't silently use different models for different call types. The user's key determines the model.
- **Support burden.** Users misconfigure keys, hit rate limits, encounter billing issues. These become support tickets for a problem the platform doesn't control.
- **Barriers to entry.** Non-technical users (Elena) will not obtain and configure an API key. This effectively locks the most compelling use case (AI-only mode for non-modelers) behind a technical prerequisite.

BYOK can work as a *secondary* option for power users (David) who want control, but it cannot be the primary delivery model.

---

## 4. Recommendation: Hybrid Architecture

The right answer is not one option — it's an architecture that supports multiple options behind the same interface.

### 4.1 Immediate (v1): Intermediary Service

Run a thin relay service as the default. Reasons:

- Scale doesn't justify self-hosting yet
- Best model quality is needed for posture system development — the prompts need to be proven against frontier models before attempting to port to weaker ones
- Service layer enables rapid prompt iteration without client updates
- Analytics are essential during early development to understand which postures work and which don't
- Prompt protection preserves competitive advantage

### 4.2 Near-term: Tiered Model Routing

Once postures are stable, implement tiered model routing server-side:

| Posture / Call Type | Model | Cost Impact |
|---|---|---|
| OBSERVER acknowledgments | Haiku-class | ~90% cheaper per call |
| Conversation summarization | Haiku-class | ~90% cheaper per call |
| ELICITOR, FORMALIZER | Sonnet-class | ~60% cheaper per call |
| ANTICIPATOR, INTERPRETER | Sonnet-class or Opus-class | Depends on complexity |
| CHALLENGER, AUTONOMIST | Opus-class | Full price |

This alone could reduce average session cost by 30–50%.

### 4.3 Medium-term: On-Premises Option for Institutional Buyers

Offer a self-hosted deployment for hospitals, universities, and government agencies that require data sovereignty. Constraints:

- Limit on-prem to the safer postures (FORMALIZER, INTERPRETER) where open-weight model quality is adequate
- Require the cloud service for AUTONOMIST mode, or accept reduced quality with appropriate labeling
- Fine-tune the on-prem model on SD domain knowledge to partially compensate for the capability gap
- Price as an enterprise license (annual fee covering the software + deployment support), not per-use

### 4.4 Architectural Requirement

The key architectural decision: **keep the LLM call behind an interface so the backend is swappable.** The prompt assembly, posture selection, and tool-use protocol must be identical regardless of whether the LLM endpoint is:

- Anthropic's API via a relay service
- A self-hosted open-weight model
- A user's own API key (BYOK mode)
- A future provider not yet known

This means the `LlmClient` interface should accept an assembled prompt and return a structured response. Everything above it (posture selection, model context serialization, conversation windowing, tool-use parsing) is provider-agnostic. Everything below it (HTTP transport, authentication, response parsing) is provider-specific and pluggable.

```
┌─────────────────────────────────┐
│  Posture System                 │
│  Maturity Signals → Posture     │
├─────────────────────────────────┤
│  Prompt Assembly                │
│  System + Posture + Model +     │
│  History + User Message         │
├─────────────────────────────────┤
│  Tool-Use Protocol              │
│  Parse tool calls, render       │
│  diff cards, await approval     │
├─────────────────────────────────┤
│  LlmClient (interface)         │  ← Swap point
├────────┬────────┬───────────────┤
│ Claude │ Self-  │ BYOK /        │
│ Relay  │ Hosted │ Other         │
└────────┴────────┴───────────────┘
```

---

## 5. Pricing Considerations

### 5.1 The Central Tension

The platform's most compelling feature — AI-only mode for non-modelers — is also the most expensive to operate per insight delivered, and targets the audience least likely to pay premium prices (hospital administrators, students, city planners vs. professional researchers).

### 5.2 Pricing Models

| Model | Pros | Cons |
|---|---|---|
| **Subscription (monthly/annual)** | Predictable revenue; users don't ration interactions | Heavy users subsidized by light users; must set usage caps or accept loss on outliers |
| **Per-session** | Cost tracks usage; fair to light users | Discourages exploration; users may abandon sessions to avoid charges |
| **Institutional license** | Bulk pricing; one purchase decision for many users | Requires sales effort; budget cycles are slow |
| **Freemium (limited free tier + paid)** | Low barrier to entry; Maya can start for free | Must define the free tier carefully — too generous and no one upgrades, too stingy and non-paying users bounce |

### 5.3 Suggested Starting Point

- **Free tier:** Canvas-based modeling with no AI (Phase 1 of the spec — the "better Vensim" baseline). Full simulation, save/load, export. No conversation panel.
- **Individual subscription:** Full AI integration, all postures, AI-only mode. Priced to cover API costs + margin. Target: $15–$30/month depending on usage tier.
- **Institutional license:** On-premises deployment option, bulk user accounts, data sovereignty guarantees. Annual pricing negotiated per institution.

The free tier serves two purposes: it establishes the tool's value independent of AI (important for credibility in the SD community, which may be skeptical of AI-assisted modeling), and it creates a conversion funnel for users who discover they want AI assistance.

---

## 6. Long-Term Cost Trajectory

### 6.1 API Pricing Trends

LLM API prices have declined roughly 10x over the past two years (2024–2026) and continue to fall. If this trend continues:

- A $3 session today becomes $0.30 in two years
- The economic argument for self-hosting weakens as API costs approach the infrastructure cost of running your own GPUs
- The fixed costs of self-hosting (GPU leases, ML ops staff, model updates) become relatively more expensive

### 6.2 Implication

The service relay model (Option A) becomes *more* attractive over time, not less. The advantages that don't erode with price drops — prompt protection, analytics, model routing, prompt iteration speed — remain valuable regardless of per-token cost.

Self-hosting remains relevant primarily for data sovereignty, not cost savings. As API prices fall, the only users who need self-hosting are those who *cannot* send data to a third party, not those trying to save money.

### 6.3 The Strategic Bet

The long-term value of the platform is in the posture system, the maturity-to-behavior mapping, and the domain-specific prompt engineering — not in the LLM itself. The LLM is a commodity input. The intelligence about *how to use it for system dynamics modeling* is the product. This argues for keeping the prompts server-side (Option A) and treating the LLM as a pluggable backend that will get cheaper and better over time regardless of what the platform does.
