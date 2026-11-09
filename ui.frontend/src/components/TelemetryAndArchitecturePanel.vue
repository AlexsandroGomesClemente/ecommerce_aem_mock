<script setup lang="ts">
import type { TelemetrySnapshot } from '../types/ecommerce';

defineProps<{
  telemetry: TelemetrySnapshot | null;
}>();

defineEmits<{
  (e: 'refresh'): void;
}>();
</script>

<template>
  <section class="telemetry-panel" data-testid="telemetry-panel">
    <div class="telemetry-panel__header">
      <div>
        <h2>Telemetria, Observabilidade & Status das ADRs</h2>
        <p>Monitoramento em tempo real (MDC Correlation-ID, Micrometer/Prometheus & SLO p95 &lt; 200ms)</p>
      </div>
      <button type="button" class="btn-secondary" @click="$emit('refresh')">
        ↻ Atualizar Telemetria
      </button>
    </div>

    <div class="adr-pills-grid">
      <div class="adr-pill">
        <strong>ADR-001 • Docker Compose</strong>
        <span>PostgreSQL + AEM + Java + Vue + Prometheus</span>
      </div>
      <div class="adr-pill">
        <strong>ADR-002 • PostgreSQL ACID + Flyway</strong>
        <span>Transações @Transactional &amp; Lock Otimista</span>
      </div>
      <div class="adr-pill">
        <strong>ADR-003 • GraphQL BFF + DataLoader</strong>
        <span>@BatchMapping Anti N+1 Queries</span>
      </div>
      <div class="adr-pill">
        <strong>ADR-004 • Vue 3 -&gt; AEM ClientLibs</strong>
        <span>ui.apps (cq:ClientLibraryFolder XML)</span>
      </div>
      <div class="adr-pill">
        <strong>ADR-005 • OAuth2 JWT Cookie HttpOnly</strong>
        <span>Stateless + Anti-XSS + Token DenyList</span>
      </div>
      <div class="adr-pill">
        <strong>ADR-006 • k6 Load Tests (500 &amp; 5000 VUs)</strong>
        <span>SLO p95 &lt; 200ms validado em CI/CD</span>
      </div>
    </div>

    <div v-if="telemetry" class="metrics-grid">
      <div class="metric-box">
        <span class="metric-label">Total de Requisições</span>
        <strong class="metric-val">{{ telemetry.totalRequests }}</strong>
      </div>
      <div class="metric-box">
        <span class="metric-label">Chamadas GraphQL (/graphql)</span>
        <strong class="metric-val">{{ telemetry.graphqlRequests }}</strong>
      </div>
      <div class="metric-box">
        <span class="metric-label">Chamadas API REST (/api/v1)</span>
        <strong class="metric-val">{{ telemetry.restRequests }}</strong>
      </div>
      <div class="metric-box">
        <span class="metric-label">Pedidos ACID Confirmados</span>
        <strong class="metric-val">{{ telemetry.ordersCreated }}</strong>
      </div>
      <div class="metric-box">
        <span class="metric-label">Latência p95 (SLO &lt; 200ms)</span>
        <strong class="metric-val" :class="{ 'slo-ok': telemetry.p95LatencyMs < 200 }">
          {{ telemetry.p95LatencyMs }} ms
        </strong>
      </div>
      <div class="metric-box">
        <span class="metric-label">Tokens na DenyList (ADR-005)</span>
        <strong class="metric-val">{{ telemetry.activeDenyListTokens }}</strong>
      </div>
    </div>

    <div v-if="telemetry?.recentLogs?.length" class="logs-table-wrapper">
      <h3>Logs Estruturados Recentes (SLF4J + MDC Correlation-ID)</h3>
      <table class="logs-table">
        <thead>
          <tr>
            <th>Horário</th>
            <th>Correlation ID</th>
            <th>Nível</th>
            <th>Componente</th>
            <th>Mensagem</th>
            <th>Latência</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(entry, idx) in telemetry.recentLogs.slice(0, 10)" :key="idx">
            <td><code>{{ entry.timestamp }}</code></td>
            <td><code>{{ entry.correlationId }}</code></td>
            <td>
              <span class="log-badge" :class="`log-${entry.level.toLowerCase()}`">
                {{ entry.level }}
              </span>
            </td>
            <td>{{ entry.component }}</td>
            <td>{{ entry.message }}</td>
            <td>{{ entry.latencyMs }} ms</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

