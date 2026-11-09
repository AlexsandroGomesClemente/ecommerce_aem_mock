package com.ecommerce.service;

import com.ecommerce.dto.ApiDtos.AemHeroBanner;
import com.ecommerce.dto.ApiDtos.AemProductEditorial;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ADR-002 e ADR-003: Conector de Conteúdo Editorial do Adobe Experience Manager (AEM JCR).
 * Separa claramente o que é "Conteúdo Editorial" (gerenciado no JCR do AEM: banners, textos ricos,
 * SEO, destaques visuais) do que é "Dado Transacional" (gerenciado no PostgreSQL: preço, estoque, pedidos).
 *
 * Fornece resolução em Lote (Batch) para o DataLoader do GraphQL, eliminando o problema de N+1 queries.
 */
@Service
public class AemContentService {

    private final Map<String, AemProductEditorial> jcrEditorialRepository = new HashMap<>();

    public AemContentService() {
        seedAemJcrContentFragments();
    }

    public AemHeroBanner getStorefrontHeroBanner() {
        return new AemHeroBanner(
                "/content/experience-fragments/ecommerce/br/pt/site/hero-banner/master",
                "AEM EXPERIENCE FRAGMENT • JCR PUBLISHED",
                "Arquitetura Composable: Vue 3 + AEM ClientLibs + Java GraphQL BFF",
                "Conteúdo editorial renderizado via Adobe Experience Manager combinado em tempo real com estoque e preços ACID do PostgreSQL em uma única query GraphQL.",
                "Explorar Catálogo Unificado",
                "linear-gradient(135deg, #0f172a 0%, #1e3a8a 55%, #0ea5e9 100%)",
                "aem-author@enterprise.com",
                "2026-11-08T21:00:00-03:00"
        );
    }

    public AemProductEditorial getEditorialByPath(String aemContentPath) {
        return jcrEditorialRepository.getOrDefault(
                aemContentPath,
                new AemProductEditorial(
                        aemContentPath,
                        "Conteúdo Editorial Sincronizado via AEM JCR",
                        "Content Fragment publicado pela equipe editorial no Adobe Experience Manager.",
                        "AEM Verified",
                        List.of("Garantia Oficial 12 Meses", "Envio Express Prime"),
                        "linear-gradient(135deg, #334155 0%, #0f172a 100%)",
                        "box",
                        "ecommerce, aem, vue, graphql"
                )
        );
    }

    /**
     * ADR-003: Busca em Lote (Batch) utilizada pelo @BatchMapping (DataLoader) do GraphQL.
     * Em vez de fazer N chamadas separadas ao AEM para cada produto listado, resolve todos os paths
     * em uma única operação agregada.
     */
    public Map<String, AemProductEditorial> fetchEditorialByPathsBatch(Collection<String> paths) {
        Map<String, AemProductEditorial> result = new HashMap<>(paths.size());
        for (String path : paths) {
            result.put(path, getEditorialByPath(path));
        }
        return result;
    }

    private void seedAemJcrContentFragments() {
        jcrEditorialRepository.put(
                "/content/dam/ecommerce/cf/products/sku-pro-ultra-16",
                new AemProductEditorial(
                        "/content/dam/ecommerce/cf/products/sku-pro-ultra-16",
                        "Performance Extrema para Engenharia de Software & IA",
                        "Projetado com chassi unibody em alumínio aeroespacial, tela Liquid Retina XDR 120Hz e 64GB de memória unificada para compilar containers Docker e microsserviços Java em segundos.",
                        "Destaque Editorial AEM",
                        List.of("Chip M3 Max 16-Core", "64GB RAM Unificada", "Bateria 22h + MagSafe"),
                        "linear-gradient(135deg, #1e293b 0%, #3b82f6 100%)",
                        "laptop",
                        "ultrabook, m3 max, dev workstation, aem content fragment"
                )
        );

        jcrEditorialRepository.put(
                "/content/dam/ecommerce/cf/products/sku-smart-5g-pro",
                new AemProductEditorial(
                        "/content/dam/ecommerce/cf/products/sku-smart-5g-pro",
                        "Fotografia Computacional ProRAW & Conectividade 5G Ultra",
                        "Acabamento em titânio grau 5 com câmera periscópica 5x, gravação ProRes 4K 120fps e modem 5G de baixa latência homologado para experiências mobile de alta velocidade.",
                        "Lançamento Black Friday",
                        List.of("Titânio Aeroespacial", "Câmera Tripla 48MP Pro", "Tela Super AMOLED 120Hz"),
                        "linear-gradient(135deg, #312e81 0%, #6366f1 100%)",
                        "smartphone",
                        "smartphone 5g, titanio, camera pro, black friday"
                )
        );

        jcrEditorialRepository.put(
                "/content/dam/ecommerce/cf/products/sku-audio-anc-900",
                new AemProductEditorial(
                        "/content/dam/ecommerce/cf/products/sku-audio-anc-900",
                        "Cancelamento de Ruído Adaptativo & Áudio Espacial Lossless",
                        "Oito microfones com beamforming e duplo processador acústico que isolam 99.4% do ruído externo. Perfeito para foco profundo em arquitetura de sistemas e chamadas executivas.",
                        "Escolha do Editor",
                        List.of("ANC Duplo Processador", "Autonomia de 40 Horas", "Codec Hi-Res LDAC / USB-C"),
                        "linear-gradient(135deg, #064e3b 0%, #10b981 100%)",
                        "headphones",
                        "headphone anc, audio espacial, studio lossless"
                )
        );

        jcrEditorialRepository.put(
                "/content/dam/ecommerce/cf/products/sku-monitor-oled-34",
                new AemProductEditorial(
                        "/content/dam/ecommerce/cf/products/sku-monitor-oled-34",
                        "Imersão UltraWide QD-OLED 21:9 com Hub Thunderbolt 4",
                        "Painel curvo 1800R com contraste infinito 1.500.000:1, tempo de resposta de 0.03ms e KVM Switch integrado para alternar entre ambientes de desenvolvimento.",
                        "Frete Grátis Prime",
                        List.of("Painel QD-OLED 240Hz", "USB-C Power Delivery 96W", "99.3% DCI-P3 Calibrado"),
                        "linear-gradient(135deg, #4c1d95 0%, #8b5cf6 100%)",
                        "monitor",
                        "monitor oled, ultrawide 34, 240hz, thunderbolt"
                )
        );

        jcrEditorialRepository.put(
                "/content/dam/ecommerce/cf/products/sku-watch-titan-2",
                new AemProductEditorial(
                        "/content/dam/ecommerce/cf/products/sku-watch-titan-2",
                        " Cristal de Safira & Telemetria Biométrica Contínua",
                        "Caixa robusta de 49mm em titânio com GPS de dupla frequência L1+L5, sensores avançados de saúde e resistência à água até 100 metros.",
                        "Novo no Catálogo",
                        List.of("GPS Dupla Frequência", "Tela Safira 3000 Nits", "eSIM 4G/LTE Integrado"),
                        "linear-gradient(135deg, #7c2d12 0%, #f97316 100%)",
                        "watch",
                        "smartwatch titanio, safira, gps preciso"
                )
        );

        jcrEditorialRepository.put(
                "/content/dam/ecommerce/cf/products/sku-key-mech-pro",
                new AemProductEditorial(
                        "/content/dam/ecommerce/cf/products/sku-key-mech-pro",
                        "Switches Óptico-Mecânicos Táteis & Firmware QMK/VIA",
                        "Construção low-profile em alumínio CNC com conexão tri-mode (2.4GHz 1000Hz, Bluetooth 5.3 e USB-C) e keycaps PBT double-shot de alta durabilidade.",
                        "Mais Vendido",
                        List.of("Switches Hot-Swappable", "Conexão Tri-Mode 1000Hz", "Compatível Mac/Windows/Linux"),
                        "linear-gradient(135deg, #134e4a 0%, #14b8a6 100%)",
                        "keyboard",
                        "teclado mecanico, wireless, qmk via, programador"
                )
        );
    }
}

