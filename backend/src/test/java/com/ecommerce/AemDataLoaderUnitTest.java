package com.ecommerce;

import com.ecommerce.dto.ApiDtos.AemProductEditorial;
import com.ecommerce.service.AemContentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AemDataLoaderUnitTest {

    @Test
    @DisplayName("ADR-003: Deve resolver múltiplos Content Fragments do AEM em lote (Batch DataLoader) evitando N+1")
    void shouldResolveAemContentFragmentsInBatch() {
        AemContentService aemContentService = new AemContentService();

        List<String> paths = List.of(
                "/content/dam/ecommerce/cf/products/sku-pro-ultra-16",
                "/content/dam/ecommerce/cf/products/sku-smart-5g-pro",
                "/content/dam/ecommerce/cf/products/sku-audio-anc-900"
        );

        Map<String, AemProductEditorial> batchResult = aemContentService.fetchEditorialByPathsBatch(paths);

        assertThat(batchResult).hasSize(3);
        assertThat(batchResult.get("/content/dam/ecommerce/cf/products/sku-pro-ultra-16").badge())
                .isEqualTo("Destaque Editorial AEM");
        assertThat(batchResult.get("/content/dam/ecommerce/cf/products/sku-smart-5g-pro").highlights())
                .contains("Titânio Aeroespacial");
    }
}

