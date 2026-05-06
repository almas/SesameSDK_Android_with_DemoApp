// Minimal Sesame Local HTTP Server
const http = require('http');
const url = require('url');
const querystring = require('querystring');

const PORT = 3000;
const devices = new Map();

const server = http.createServer((req, res) => {
  const parsedUrl = url.parse(req.url, true);
  const pathname = parsedUrl.pathname;

  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Content-Type', 'application/json');

  if (pathname === '/health') {
    res.writeHead(200);
    res.end(JSON.stringify({ status: 'ok' }));
  } else if (pathname === '/device/list') {
    res.writeHead(200);
    res.end(JSON.stringify(Array.from(devices.values())));
  } else if (pathname.startsWith('/device/v1/sesame5/')) {
    res.writeHead(200);
    res.end(JSON.stringify({ success: true }));
  } else {
    res.writeHead(200);
    res.end(JSON.stringify({ success: true }));
  }
});

server.listen(PORT, () => {
  console.log(`Sesame Local Server running on http://localhost:${PORT}`);
});

