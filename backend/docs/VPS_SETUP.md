# PixelVault VPS Setup Guide

Step-by-step guide to deploying PixelVault on Ubuntu 22.04 LTS.

## Prerequisites

- Ubuntu 22.04 LTS VPS (minimum 2 vCPU, 4 GB RAM, 40 GB SSD)
- A domain name pointed to your server's IP
- SSH access as root or a sudo user

---

## 1. Initial Server Setup

```bash
# Update system packages
apt update && apt upgrade -y

# Create a non-root user
adduser pixelvault
usermod -aG sudo pixelvault

# Switch to new user
su - pixelvault
```

---

## 2. Configure Firewall (UFW)

```bash
# Install and enable UFW
apt install -y ufw

# Allow SSH, HTTP, HTTPS
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp

# Enable firewall
ufw enable

# Verify
ufw status
```

---

## 3. Install Docker and Docker Compose

```bash
# Install prerequisites
apt install -y ca-certificates curl gnupg lsb-release

# Add Docker's official GPG key
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
  gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

# Add the repository
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | \
  tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker Engine
apt update
apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Allow current user to run docker without sudo
usermod -aG docker $USER
newgrp docker

# Verify
docker --version
docker compose version
```

---

## 4. Configure DNS

In your DNS provider's control panel, add the following A records:

| Type | Name | Value |
|------|------|-------|
| A | `yourdomain.com` | `<your-server-ip>` |
| A | `minio.yourdomain.com` | `<your-server-ip>` |
| A | `s3.yourdomain.com` | `<your-server-ip>` |

Wait for DNS propagation (can take up to 24 hours, usually minutes).

Verify:
```bash
nslookup yourdomain.com
```

---

## 5. Clone and Configure PixelVault

```bash
# Clone the repository
git clone https://github.com/yourorg/pixelvault.git /opt/pixelvault
cd /opt/pixelvault

# Copy and edit environment file
cp .env.example .env
nano .env
```

Update the following values in `.env`:

```env
DATABASE_URL=postgres://pixelvault:STRONG_PASSWORD@db:5432/pixelvault?sslmode=disable
POSTGRES_PASSWORD=STRONG_PASSWORD
JWT_SECRET=<generate with: openssl rand -hex 32>
SIGNED_URL_SECRET=<generate with: openssl rand -hex 32>
MINIO_ACCESS_KEY=<your-minio-user>
MINIO_SECRET_KEY=<strong-minio-password>
STORAGE_TYPE=minio
ALLOWED_ORIGINS=https://yourdomain.com
```

Generate strong secrets:
```bash
openssl rand -hex 32   # for JWT_SECRET
openssl rand -hex 32   # for SIGNED_URL_SECRET
```

---

## 6. Configure Caddy

Edit `Caddyfile` and replace `yourdomain.com` with your actual domain:

```bash
sed -i 's/yourdomain.com/YOUR_ACTUAL_DOMAIN/g' Caddyfile
```

---

## 7. Deploy PixelVault

```bash
cd /opt/pixelvault

# Build and start all services
docker compose up -d --build

# Monitor startup logs
docker compose logs -f

# Check all containers are running
docker compose ps
```

All services should show as `healthy` after ~30 seconds.

---

## 8. Verify SSL and Deployment

```bash
# Test HTTP (should redirect to HTTPS)
curl -I http://yourdomain.com

# Test HTTPS API
curl https://yourdomain.com/api/health

# Test MinIO Console is accessible
curl -I https://minio.yourdomain.com
```

---

## 9. Create the First Admin User

```bash
# Register first user via API
curl -X POST https://yourdomain.com/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "admin@yourdomain.com",
    "password": "your-strong-password",
    "device_name": "admin-cli",
    "device_type": "cli"
  }'

# Promote to admin using the database
docker compose exec db psql -U pixelvault -c \
  "UPDATE users SET role='admin' WHERE email='admin@yourdomain.com';"
```

---

## 10. Configure Automatic Backups

### Database Backup

```bash
# Create backup script
cat > /opt/pixelvault/backup.sh << 'EOF'
#!/bin/bash
BACKUP_DIR=/opt/pixelvault/backups
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR
docker compose -f /opt/pixelvault/docker-compose.yml exec -T db \
  pg_dump -U pixelvault pixelvault | gzip > $BACKUP_DIR/db_$DATE.sql.gz
# Keep last 30 backups
find $BACKUP_DIR -name 'db_*.sql.gz' -mtime +30 -delete
EOF
chmod +x /opt/pixelvault/backup.sh

# Schedule daily backups at 3 AM
(crontab -l 2>/dev/null; echo "0 3 * * * /opt/pixelvault/backup.sh") | crontab -
```

---

## 11. Automatic Updates and Maintenance

```bash
# Enable unattended security updates
apt install -y unattended-upgrades
dpkg-reconfigure -plow unattended-upgrades

# Configure automatic container log rotation
cat > /etc/docker/daemon.json << 'EOF'
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
EOF
systemctl restart docker
```

---

## 12. Updating PixelVault

```bash
cd /opt/pixelvault

# Pull latest changes
git pull origin main

# Rebuild and restart
docker compose up -d --build

# View logs
docker compose logs -f backend
```

---

## Troubleshooting

### Container won't start
```bash
docker compose logs backend
docker compose logs db
```

### Database connection issues
```bash
docker compose exec db psql -U pixelvault -c '\l'
```

### Caddy certificate issues
```bash
docker compose exec caddy caddy validate --config /etc/caddy/Caddyfile
docker compose logs caddy
```

### Reset everything (⚠️ destroys data)
```bash
docker compose down -v
docker compose up -d --build
```

---

## Security Checklist

- [ ] Strong, unique passwords for DB, MinIO, and JWT secrets
- [ ] UFW firewall enabled with minimal open ports
- [ ] SSH key authentication only (disable password auth)
- [ ] Regular database backups configured
- [ ] Domain pointing correctly with SSL working
- [ ] `ALLOWED_ORIGINS` set to your actual frontend domain
- [ ] MinIO console port (9001) NOT exposed to the internet
- [ ] Regular `apt update && apt upgrade` scheduled
