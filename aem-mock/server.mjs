import http from 'node:http';

const PORT = process.env.PORT || 4502;

const server = http.createServer((req, res) => {
  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  if (req.url === '/system/health') {
    res.writeHead(200);
    res.end(JSON.stringify({ status: 'OK', instance: 'AEM-Publish-JCR-Simulator', port: PORT }));
    return;
  }

  res.writeHead(200);
  res.end(
    JSON.stringify({
      jcrRepository: 'Adobe Experience Manager (JCR Content Services)',
      clientLibCategory: 'ecommerce.site',
      experienceFragment: '/content/experience-fragments/ecommerce/br/pt/site/hero-banner/master',
      status: 'PUBLISHED'
    })
  );
});

server.listen(PORT, () => {
  console.log(`[AEM JCR Service Mock] Rodando na porta ${PORT}`);
});

