# RESTful API Production Deployment

## Production Checklist

- [x] SQL injection protection
- [x] API key authentication
- [x] Environment variables secured
- [x] Error handling (no stack traces exposed)
- [x] Database connection pooling
- [x] Logging configuration
- [x] Docker support
- [x] Nginx reverse proxy
- [x] Rate limiting (10 req/s via Nginx)
- [x] Security headers
- [x] Health check endpoint
- [ ] HTTPS/TLS configuration (manual setup required)
- [ ] CORS configuration (if needed)
- [ ] Monitoring/metrics

## Deployment Options

### Option 1: Docker Compose with Nginx (Recommended)
```bash
# Optional: Set up SSL certificates first
chmod +x setup-ssl.sh
./setup-ssl.sh

# Build and run with Docker
docker-compose -f docker-compose-prod.yml up -d

# View logs
docker-compose -f docker-compose-prod.yml logs -f

# Stop services
docker-compose -f docker-compose-prod.yml down
```

### Option 2: JAR File
```bash
# Make deployment script executable
chmod +x deploy.sh

# Deploy
./deploy.sh
```

### Option 3: Development
```bash
# Make run script executable
chmod +x run.sh

# Run in development mode
./run.sh
```

## API Authentication

All endpoints require an `X-API-Key` header:

```bash
# With Nginx (port 80)
curl -H "X-API-Key: your-api-key-here" http://localhost/superior/health

# Direct to API (port 8080, without Nginx)
curl -H "X-API-Key: your-api-key-here" http://localhost:8080/superior/health
```

## Nginx Configuration

**Features enabled:**
- Rate limiting: 10 requests/second per IP
- Security headers (X-Frame-Options, X-Content-Type-Options, etc.)
- Proxy to Spring Boot application
- Health check endpoint (no rate limiting)
- HTTPS support (requires SSL setup)

## Environment Variables Required

Create a `.env` file with:
```
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password
API_KEY=your_generated_api_key
DB_HOST=localhost
DB_PORT=5432
DB_NAME=superior
```

## SSL/HTTPS Setup

### For Production with Domain:
```bash
chmod +x setup-ssl.sh
./setup-ssl.sh
# Choose option 2 for Let's Encrypt
```

### For Testing (Self-Signed):
```bash
./setup-ssl.sh
# Choose option 1 for self-signed certificate
```

Then uncomment the HTTPS server block in `nginx.conf` and update `server_name`.

## Security Recommendations

1. **Change the API key** - Generate a new one with:
   ```bash
   openssl rand -hex 32
# Through Nginx
curl http://localhost/superior/health

# Direct to API
curl http://localhost:8080/superior/health
```

Expected response: `{"status":"UP"}`

## Testing the Setup

```bash
# Test rate limiting (should get 429 after 10 requests/second)
for i in {1..15}; do curl -H "X-API-Key: ${API_KEY}" http://localhost/superior/health; done

# Test API endpoint
curl -H "X-API-Key: ${API_KEY}" http://localhost/superior/images/48-11-1850

# View Nginx logs
docker-compose -f docker-compose-prod.yml logs nginx

# View API logs
docker-compose -f docker-compose-prod.yml logs api
``
3. **Enable HTTPS** - Run `./setup-ssl.sh` and configure SSL

4. **Rate limiting is enabled** - 10 req/s per IP (configurable in nginx.conf)

5. **Set up monitoring** and alerts

6. **Regular backups** of PostgreSQL database

7. **Update dependencies** regularly for security patches

8. **Update Nginx server_name** in nginx.conf to your domain

9. **Firewall configuration** - Only allow ports 80, 443, and SSH

## Health Check

```bash
curl http://localhost:8080/superior/health
```

Expected response: `{"status":"UP"}`
