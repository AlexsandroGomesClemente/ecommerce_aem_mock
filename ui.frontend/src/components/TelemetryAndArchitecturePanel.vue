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
  <div class="telemetry-view" data-testid="telemetry-panel">
    <div class="telemetry-view__header">
      <div>
        <h2>Painel de Telemetria</h2>
        <p>Monitoramento e métricas de desempenho em tempo real</p>
      </div>
      <button type="button" class="btn-secondary" @click="$emit('refresh')">
        ↻ Atualizar Métricas
      </button>
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
        <span class="metric-label">Chamadas REST (/api/v1)</span>
        <strong class="metric-val">{{ telemetry.restRequests }}</strong>
      </div>
      <div class="metric-box">
        <span class="metric-label">Pedidos Confirmados</span>
        <strong class="metric-val">{{ telemetry.ordersCreated }}</strong>
      </div>
      <div class="metric-box">
        <span class="metric-label">Latência Média</span>
        <strong class="metric-val">{{ telemetry.avgLatencyMs }} ms</strong>
      </div>
      <div class="metric-box">
        <span class="metric-label">Latência p95</span>
        <strong class="metric-val" :class="{ 'slo-ok': telemetry.p95LatencyMs < 200 }">
          {{ telemetry.p95LatencyMs }} ms
        </strong>
      </div>
    </div>

    <div v-if="telemetry?.recentLogs?.length" class="logs-table-wrapper">
      <h3>Histórico de Requisições e Logs</h3>
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
          <tr v-for="(entry, idx) in telemetry.recentLogs.slice(0, 15)" :key="idx">
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
  </div>
</template>
