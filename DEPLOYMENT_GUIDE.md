# AI Test Management Tool - Server Deployment Guide

## Overview
When deploying the JAR file to a production server, you need to configure specific environment variables and properties. This guide covers all required configurations.

---

## Pre-Deployment Checklist

### 1. **Database Setup**
- [ ] PostgreSQL database is running and accessible
- [ ] Database name: `pgvector` (or your custom name)
- [ ] Database user created with appropriate permissions
- [ ] Database credentials are secure

### 2. **Environment Configuration**
- [ ] Copy `.env.example` to `.env` 
- [ ] Update all placeholders in `.env` with production values
- [ ] Ensure `.env` is NOT committed to version control
- [ ] Set proper file permissions on `.env` (chmod 600)

### 3. **Security Configuration**
- [ ] Generate encryption key: `openssl rand -base64 32`
- [ ] Update `ENCRYPTION_SECRET_KEY` in `.env`
- [ ] Update `CORS_ALLOWED_ORIGINS` with your frontend URL
- [ ] Generate JWT secret (if using authentication)

### 4. **Server Configuration**
- [ ] Decide on server port (default: 8080)
- [ ] Configure SSL/HTTPS if needed
- [ ] Setup reverse proxy (Nginx/Apache) if required
- [ ] Configure firewall rules

---

## Required Environment Variables

### Server & Database
```properties
SERVER_PORT=8080                              # Application port
SPRING_PROFILE=prod                           # Spring profile (dev/prod)
DB_HOST=your-database-server.com              # Database hostname
DB_PORT=5432                                  # PostgreSQL port
DB_NAME=pgvector                              # Database name
DB_USER=postgres_user                         # Database username
DB_PASSWORD=your-secure-password              # Database password
```

### Security
```properties
ENCRYPTION_SECRET_KEY=your-base64-key         # Generated encryption key
SESSION_COOKIE_SECURE=true                    # Set to true for HTTPS
CORS_ALLOWED_ORIGINS=https://yourdomain.com  # Frontend URL(s)
```

### Optional: Integrations
```properties
# JIRA Integration
JIRA_BASE_URL=https://jira.atlassian.net
JIRA_API_TOKEN=your-token
JIRA_USERNAME=username

# Azure OpenAI
AZURE_OPENAI_ENABLED=true
AZURE_OPENAI_API_KEY=your-key
AZURE_OPENAI_ENDPOINT=https://resource.openai.azure.com

# OpenAI (if not using Azure)
OPENAI_API_KEY=sk-your-key
```

---

## Deployment Steps

### 1. Build the JAR
```bash
mvn clean package -DskipTests
```
Output: `target/ai-testmanagement-tool-2.0-SNAPSHOT.jar`

### 2. Prepare Server Environment
```bash
# Create application directory
mkdir -p /opt/test-management-tool
cd /opt/test-management-tool

# Copy JAR and configuration
cp /path/to/ai-testmanagement-tool-2.0-SNAPSHOT.jar .
cp /path/to/.env .
chmod 600 .env
```

### 3. Create SystemD Service (Linux)
Create `/etc/systemd/system/test-management.service`:
```ini
[Unit]
Description=AI Test Management Tool
After=network.target

[Service]
Type=simple
User=appuser
WorkingDirectory=/opt/test-management-tool
EnvironmentFile=/opt/test-management-tool/.env
ExecStart=/usr/bin/java -Xmx2g -jar ai-testmanagement-tool-2.0-SNAPSHOT.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
sudo systemctl daemon-reload
sudo systemctl enable test-management
sudo systemctl start test-management
```

### 4. Verify Deployment
```bash
# Check service status
sudo systemctl status test-management

# View logs
sudo journalctl -u test-management -f

# Test API
curl http://localhost:8080/api/v3/api-docs
curl http://localhost:8080/api/health
```

---

## Important Application Properties

### application.properties
- **Database**: Connection pooling configured (HikariCP)
- **Logging**: Configured to write to `logs/application.log`
- **JPA/Hibernate**: DDL set to `validate` (no schema changes)

### application-integration.properties
- **Security**: Encryption and session management
- **CORS**: Restricted origins (update for production)
- **Swagger**: Disabled by default (enable only in dev)
- **Cache**: TTL configured for 5 minutes

---

## Production Best Practices

### Security
- [ ] Use HTTPS/SSL in production
- [ ] Set `SESSION_COOKIE_SECURE=true`
- [ ] Disable Swagger UI: `SWAGGER_ENABLED=false`
- [ ] Use strong database passwords
- [ ] Rotate encryption keys periodically
- [ ] Keep `.env` file secure with restricted permissions

### Performance
- [ ] Configure appropriate JVM heap size (`-Xmx2g`)
- [ ] Enable connection pooling
- [ ] Monitor database connections
- [ ] Setup log rotation

### Monitoring
- [ ] Setup application logging and monitoring
- [ ] Configure alerting for errors
- [ ] Monitor database performance
- [ ] Track API response times

### Backup & Recovery
- [ ] Regular database backups
- [ ] Document recovery procedures
- [ ] Test restore procedures

---

## Troubleshooting

### Database Connection Issues
- Verify database is running and accessible
- Check credentials in `.env`
- Verify firewall rules allow connection

### Port Already in Use
```bash
# Find and kill process using port 8080
lsof -i :8080
kill -9 <PID>
```

### Out of Memory
Increase JVM heap size:
```bash
java -Xmx4g -jar ai-testmanagement-tool-2.0-SNAPSHOT.jar
```

### CORS Errors
Update `CORS_ALLOWED_ORIGINS` to include your frontend URL

### Encryption Key Issues
Ensure `ENCRYPTION_SECRET_KEY` is properly formatted (base64)

---

## Health Check Endpoints

Once deployed, verify the application:

```bash
# Health check
curl http://localhost:8080/api/actuator/health

# API documentation
curl http://localhost:8080/api/v3/api-docs
```

---

## Support

For issues or questions, refer to:
- Application logs: `logs/application.log`
- Spring Boot documentation: https://spring.io/projects/spring-boot
- PostgreSQL documentation: https://www.postgresql.org/docs/
