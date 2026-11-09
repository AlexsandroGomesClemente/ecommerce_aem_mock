/**
 * ADR-004: Pipeline de Build do Front-end (Vue.js no AEM)
 *
 * Este script é executado automaticamente após o `vite build`.
 * Ele lê os arquivos JS e CSS gerados (com hash) em `ui.frontend/dist/assets`,
 * copia-os para a estrutura de pacote JCR (`ui.apps`) e gera os arquivos:
 *   - `.content.xml` (definição de `cq:ClientLibraryFolder`)
 *   - `js.txt` (mapeamento dos bundles JavaScript)
 *   - `css.txt` (mapeamento das folhas de estilo)
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const distAssetsDir = path.resolve(__dirname, '../dist/assets');
const clientLibRoot = path.resolve(
  __dirname,
  '../../ui.apps/src/main/content/jcr_root/apps/ecommerce/clientlibs/clientlib-site'
);

const jsTargetDir = path.join(clientLibRoot, 'js');
const cssTargetDir = path.join(clientLibRoot, 'css');

fs.mkdirSync(jsTargetDir, { recursive: true });
fs.mkdirSync(cssTargetDir, { recursive: true });

for (const oldFile of fs.readdirSync(jsTargetDir)) {
  fs.unlinkSync(path.join(jsTargetDir, oldFile));
}
for (const oldFile of fs.readdirSync(cssTargetDir)) {
  fs.unlinkSync(path.join(cssTargetDir, oldFile));
}

const assetFiles = fs.existsSync(distAssetsDir) ? fs.readdirSync(distAssetsDir) : [];
const jsFiles = [];
const cssFiles = [];

for (const file of assetFiles) {
  const srcPath = path.join(distAssetsDir, file);
  if (file.endsWith('.js')) {
    fs.copyFileSync(srcPath, path.join(jsTargetDir, file));
    jsFiles.push(file);
  } else if (file.endsWith('.css')) {
    fs.copyFileSync(srcPath, path.join(cssTargetDir, file));
    cssFiles.push(file);
  }
}

const jsTxtContent = `#base=js\n${jsFiles.join('\n')}\n`;
const cssTxtContent = `#base=css\n${cssFiles.join('\n')}\n`;

const contentXml = `<?xml version="1.0" encoding="UTF-8"?>
<!-- ADR-004: Gerado automaticamente pelo pipeline Vite -> AEM ClientLibs -->
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:ClientLibraryFolder"
          allowProxy="{Boolean}true"
          categories="[ecommerce.site,ecommerce.vue.spa]"
          dependencies="[aem.core.wcm.components.commons.site]"/>
`;

fs.writeFileSync(path.join(clientLibRoot, 'js.txt'), jsTxtContent, 'utf-8');
fs.writeFileSync(path.join(clientLibRoot, 'css.txt'), cssTxtContent, 'utf-8');
fs.writeFileSync(path.join(clientLibRoot, '.content.xml'), contentXml, 'utf-8');

console.log('[ADR-004 AEM ClientLib Generator] Pacote ClientLibs atualizado com sucesso em:');
console.log('  ->', clientLibRoot);
console.log('  -> JS mapeados:', jsFiles);
console.log('  -> CSS mapeados:', cssFiles);

