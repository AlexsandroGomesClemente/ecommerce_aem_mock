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
 * imagens, badges, SEO) do que é "Dado Transacional" (gerenciado no PostgreSQL: preço, estoque, pedidos).
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
                "NOVA COLEÇÃO TECH & DESIGN",
                "Tecnologia de Alta Performance para o seu Dia a Dia",
                "Descubra produtos selecionados com entrega rápida, garantia oficial e a melhor experiência de compra.",
                "Explorar Produtos",
                "linear-gradient(135deg, #72b789 0%, #95c9a6 45%, #b8dbc4 100%)",
                "aem-author@enterprise.com",
                "2026-11-09T10:00:00-03:00"
        );
    }

    public AemProductEditorial getEditorialByPath(String aemContentPath) {
        return jcrEditorialRepository.getOrDefault(
                aemContentPath,
                new AemProductEditorial(
                        aemContentPath,
                        "Produto Selecionado QuantumStore",
                        "Item verificado com qualidade garantida e suporte prioritário.",
                        "Destaque",
                        List.of("Garantia Oficial 12 Meses", "Envio Express Prime"),
                        "linear-gradient(135deg, #dcede1 0%, #ffffff 100%)",
                        "box",
                        "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80",
                        "ecommerce, tecnologia, qualidade"
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
                        "Performance Extrema para Engenharia & Criatividade",
                        "Equipado com processador de última geração, tela Liquid Retina XDR 120Hz e chassi unibody ultra resistente para máxima produtividade.",
                        "Destaque Editorial AEM",
                        List.of("Chip M3 Max 16-Core", "64GB RAM Unificada", "Bateria até 22h"),
                        "linear-gradient(135deg, #dcede1 0%, #ffffff 100%)",
                        "laptop",
                        "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=600&q=80",
                        "ultrabook, notebook pro, engenharia, design"
                )
        );

        jcrEditorialRepository.put(
                "/content/dam/ecommerce/cf/products/sku-smart-5g-pro",
                new AemProductEditorial(
                        "/content/dam/ecommerce/cf/products/sku-smart-5g-pro",
                        "Fotografia Computacional & Conectividade 5G",
                        "Corpo em titânio aeroespacial, sistema de câmeras avançado com zoom óptico 5x e tela Super AMOLED com cores ultra vibrantes.",
                        "Lançamento",
                        List.of("Titânio Aeroespacial", "Câmera Tripla 48MP Pro", "Tela Super AMOLED 120Hz"),
                        "linear-gradient(135deg, #dcede1 0%, #ffffff 100%)",
                        "smartphone",
                        "https://images.unsplash.com/photo-1511707171634-5f897ff02560?auto=format&fit=crop&w=600&q=80",
                        "smartphone 5g, celular pro, fotografia, titanio"
                )
        );

        jcrEditorialRepository.put(
                "/content/dam/ecommerce/cf/products/sku-audio-anc-900",
                new AemProductEditorial(
                        "/content/dam/ecommerce/cf/products/sku-audio-anc-900",
                        "Cancelamento Ativo de Ruído & Som de Alta Resolução",
                        "Isolamento acústico inteligente com drivers de alta fidelidade e áudio espacial imersivo para total foco no trabalho e música.",
                        "Mais Vendido",
                        List.of("Cancelamento Ativo de Ruído", "Bateria de 40 Horas", "Codec Hi-Res Audio"),
                        "linear-gradient(135deg, #dcede1 0%, #ffffff 100%)",
                        "headphones",
                        "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80",
                        "headphone anc, fone bluetooth, cancelamento ruido"
                )
        );

        jcrEditorialRepository.put(
                "/content/dam/ecommerce/cf/products/sku-monitor-oled-34",
                new AemProductEditorial(
                        "/content/dam/ecommerce/cf/products/sku-monitor-oled-34",
                        "Imersão UltraWide Curvo com Cores Precisas",
                        "Painel curvo com contraste infinito, taxa de atualização de 240Hz e conexão única USB-C com suporte a carregamento e vídeo.",
                        "Frete Grátis",
                        List.of("Painel Curvo 240Hz", "USB-C Power Delivery 96W", "99.3% Cores DCI-P3"),
                        "linear-gradient(135deg, #dcede1 0%, #ffffff 100%)",
                        "monitor",
                        "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=600&q=80",
                        "monitor ultrawide, tela oled, setup gamer, produtividade"
                )
        );

        jcrEditorialRepository.put(
                "/content/dam/ecommerce/cf/products/sku-watch-titan-2",
                new AemProductEditorial(
                        "/content/dam/ecommerce/cf/products/sku-watch-titan-2",
                        "Cristal de Safira & Monitoramento de Saúde 24h",
                        "Resistência superior com GPS de dupla frequência, sensores biométricos de última geração e bateria de longa duração para treinos e esportes.",
                        "Novo",
                        List.of("GPS Dupla Frequência", "Vidro Safira Anti-risco", "Resistente à Água 100m"),
                        "linear-gradient(135deg, #dcede1 0%, #ffffff 100%)",
                        "watch",
                        "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80",
                        "smartwatch, relogio titanio, esportes, saude"
                )
        );

        jcrEditorialRepository.put(
                "/content/dam/ecommerce/cf/products/sku-key-mech-pro",
                new AemProductEditorial(
                        "/content/dam/ecommerce/cf/products/sku-key-mech-pro",
                        "Digitação Precisa & Conectividade Sem Fio Tri-Mode",
                        "Teclas mecânicas táteis confortáveis, estrutura em alumínio e iluminação suave para longas jornadas de programação e digitação.",
                        "Escolha do Editor",
                        List.of("Switches Mecânicos", "Conexão Bluetooth e 2.4GHz", "Compatível Mac e Windows"),
                        "linear-gradient(135deg, #dcede1 0%, #ffffff 100%)",
                        "keyboard",
                        "https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=600&q=80",
                        "teclado mecanico, teclado wireless, setup clean"
                )
        );
    }
}
