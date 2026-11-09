-- ADR-002: Dados Transacionais Iniciais (Usuários e Catálogo com vínculo ao JCR do AEM)
-- Senha padrão para ambos os usuários de demonstração: "Senha@123" (hash BCrypt)

INSERT INTO users (name, email, password_hash, role, plan_tier) VALUES
('Alex Candidato (Cliente Prime)', 'alex@ecommerce.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'ROLE_CUSTOMER', 'PRIME_BLACK'),
('Gestor Entrevistador (Admin)', 'gestor@ecommerce.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'ROLE_ADMIN', 'ENTERPRISE_ADMIN');

INSERT INTO products (sku, name, category, price, stock, rating, aem_content_path) VALUES
('SKU-PRO-ULTRA-16', 'Ultrabook Quantum Pro 16" M3 Max', 'Notebooks', 14999.90, 450, 4.9, '/content/dam/ecommerce/cf/products/sku-pro-ultra-16'),
('SKU-SMART-5G-PRO', 'Smartphone Horizon X15 Pro 5G 512GB', 'Smartphones', 6899.00, 1200, 4.8, '/content/dam/ecommerce/cf/products/sku-smart-5g-pro'),
('SKU-AUDIO-ANC-900', 'Headphone Studio ANC Spatial 900', 'Audio', 2199.50, 800, 4.9, '/content/dam/ecommerce/cf/products/sku-audio-anc-900'),
('SKU-MONITOR-OLED-34', 'Monitor Curvo UltraWide OLED 34" 240Hz', 'Monitores', 5490.00, 320, 4.7, '/content/dam/ecommerce/cf/products/sku-monitor-oled-34'),
('SKU-WATCH-TITAN-2', 'Smartwatch Titan Ultra 2 Sapphire LTE', 'Wearables', 3299.00, 650, 4.8, '/content/dam/ecommerce/cf/products/sku-watch-titan-2'),
('SKU-KEY-MECH-PRO', 'Teclado Mecânico Wireless Low-Profile RGB', 'Perifericos', 899.90, 950, 4.6, '/content/dam/ecommerce/cf/products/sku-key-mech-pro');

